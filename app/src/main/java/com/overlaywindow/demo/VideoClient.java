package com.overlaywindow.demo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;


public class VideoClient {
    private static String TAG = "VideoClient";
    private String HOST = "127.0.0.1";
    private int PORT = 8403;
    private int TIMEOUT = 3000;
    private Thread mThread;
    private MediaDecoder mMediaDecoder;
    private Surface mSurface;

    public VideoClient() {
        mMediaDecoder = new MediaDecoder();
    }

    public void setRemoteHost(String remoteHost) {
        this.HOST = remoteHost;
    }

    public void start(Surface surface) {
        this.mSurface = surface;

        mThread = new VideoClientThread();
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

    class VideoClientThread extends Thread {
        private Socket mSocket;
        public VideoClientThread() {
            super("VideoClientThread");
            Log.i(TAG, "VideoClientThread");
        }

        @Override
        public void run() {
            try {
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
                Log.d(TAG, "VideoClientThread connect success");

                decode();
            } catch (Exception e) {
                Log.i(TAG, "socket exception:" + e);
                e.printStackTrace();
            } finally {
                try {
                    if (mSocket != null) {
                        mSocket.close();
                    }

                    mMediaDecoder.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void decode() throws Exception {
            byte[] codecBuffer = new byte[0];
            byte[] headerBuffer = new byte[20];

            InputStream inputStream = mSocket.getInputStream();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (!Thread.currentThread().isInterrupted()) {
                Utils.recvBuffer(inputStream, headerBuffer, headerBuffer.length);

                ByteBuffer header = ByteBuffer.wrap(headerBuffer, 0, headerBuffer.length);
                bufferInfo.flags = header.getInt();
                bufferInfo.offset = header.getInt();
                bufferInfo.presentationTimeUs = header.getLong();
                bufferInfo.size = header.getInt();

                if (codecBuffer.length < bufferInfo.size) {
                    Log.i(TAG, "codecBuffer.length:" + codecBuffer.length + " < bufferInfo.size:" + bufferInfo.size);
                    codecBuffer = new byte[bufferInfo.size];
                }

                Utils.recvBuffer(inputStream, codecBuffer, bufferInfo.size);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.i(TAG, "BUFFER_FLAG_CODEC_CONFIG");

                    Utils.recvBuffer(inputStream, headerBuffer, 8);

                    ByteBuffer sizeBuffer = ByteBuffer.wrap(headerBuffer, 0, 8);
                    int width = sizeBuffer.getInt();
                    int height = sizeBuffer.getInt();
                    Log.i(TAG, "sizeBuffer width:" + width + " height:" + height);

                    mMediaDecoder.interrupt();

                    ByteBuffer csd0 = ByteBuffer.wrap(codecBuffer, 0, bufferInfo.size);
                    mMediaDecoder.configure(width, height, csd0, mSurface);

                    mMediaDecoder.start();
                } else {
                    mMediaDecoder.decode(codecBuffer, bufferInfo);
                }
            }
        }
    }
}


