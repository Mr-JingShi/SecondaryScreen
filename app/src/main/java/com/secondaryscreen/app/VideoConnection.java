package com.secondaryscreen.app;

import android.media.MediaCodec;
import android.util.Log;
import android.view.Surface;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class VideoConnection {
    private static String TAG = "VideoConnection";
    private Thread mThread;
    private MediaDecoder mMediaDecoder;
    private Surface mSurface;
    private DisplayConnection mDisplayConnection;

    public VideoConnection() {
        mMediaDecoder = new MediaDecoder();
        mThread = new ServerChannelThread();
    }

    public void start(Surface surface, DisplayConnection displayConnection) {
        this.mSurface = surface;
        this.mDisplayConnection = displayConnection;
        mThread.start();
    }

    public void shutdown() {
        try {
            if (mThread != null
                && mThread.isAlive()
                && !mThread.isInterrupted()) {
                mThread.interrupt();
                mThread.join();
                mThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ServerChannelThread extends Thread {
        public ServerChannelThread() {
            super("ServerChannelThread");
            Log.i(TAG, "ServerChannelThread");
        }
        @Override
        public void run() {
            try (Selector selector = Selector.open();
                 ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
                serverSocket.socket().bind(new InetSocketAddress(Utils.VIDEO_CHANNEL_PORT));

                Log.i(TAG, "ServerChannelThread bind success");

                mDisplayConnection.start();

                serverSocket.socket().setReuseAddress(true);
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                ByteBuffer headerBuffer = ByteBuffer.allocate(20);
                ByteBuffer eventBuffer = ByteBuffer.allocate(0);

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                while (!Thread.currentThread().isInterrupted()) {
                    if (selector.select() != 0) {
                        Set keys = selector.selectedKeys();
                        Iterator iterator = keys.iterator();

                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            iterator.remove();

                            if (key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                                SocketChannel socketChannel = server.accept();
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ);
                            } else if (key.isReadable()) {
                                SocketChannel socketChannel = (SocketChannel) key.channel();

                                recv(socketChannel, headerBuffer, 20);

                                bufferInfo.flags = headerBuffer.getInt();
                                bufferInfo.offset = headerBuffer.getInt();
                                bufferInfo.presentationTimeUs = headerBuffer.getLong();
                                bufferInfo.size = headerBuffer.getInt();

                                if (eventBuffer.capacity() < bufferInfo.size) {
                                    Log.i(TAG, "need bigger len:" + bufferInfo.size);
                                    eventBuffer = ByteBuffer.allocate(bufferInfo.size);
                                }

                                recv(socketChannel, eventBuffer, bufferInfo.size);

                                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    Log.i(TAG, "BUFFER_FLAG_CODEC_CONFIG");

                                    recv(socketChannel, headerBuffer, 8);

                                    int width = headerBuffer.getInt();
                                    int height = headerBuffer.getInt();
                                    Log.i(TAG, "sizeBuffer width:" + width + " height:" + height);

                                    mMediaDecoder.reset(Thread.currentThread());

                                    mMediaDecoder.configure(width, height, eventBuffer, mSurface);

                                    mMediaDecoder.start();
                                } else {
                                    mMediaDecoder.decode(eventBuffer, bufferInfo);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "ServerChannelThread exception:" + e);

                // 临时判断socket是否断联，如果断联则退出APP
                System.exit(0);
            }
        }

        private void recv(SocketChannel socketChannel, ByteBuffer buffer, int length) throws Exception {
            buffer.clear();
            buffer.limit(length);
            while (buffer.position() != length) {
                int len = socketChannel.read(buffer);
                if (len == -1) {
                    throw new RuntimeException("socket closed");
                }
            }
            buffer.flip();
        }
    }
}


