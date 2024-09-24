package com.overlaywindow.demo;

import android.media.MediaCodec;
import android.util.Log;
import android.view.Surface;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;


public class VideoClient {
    private static String TAG = "VideoClient";
    private static String HOST = "127.0.0.1";
    private static int PORT = 8403;
    private static int TIMEOUT = 3000;
    private Thread mThread;
    private MediaDecoder mMediaDecoder;
    private Surface mSurface;

    public VideoClient() {
        mMediaDecoder = new MediaDecoder();
    }

    public void start(Surface surface) {
        this.mSurface = surface;

        mThread = new VideoClientThread();
        mThread.start();
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

                Log.i(TAG, "VideoClientThread connect");

                decode();
            } catch (Exception e) {
                Log.i(TAG, "socket exception:" + e);
            } finally {
                try {
                    if (mSocket != null) {
                        mSocket.close();
                    }

                    mMediaDecoder.stop();
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
                recv(inputStream, headerBuffer, headerBuffer.length);

                ByteBuffer header = ByteBuffer.wrap(headerBuffer, 0, headerBuffer.length);
                bufferInfo.flags = header.getInt();
                bufferInfo.offset = header.getInt();
                bufferInfo.presentationTimeUs = header.getLong();
                bufferInfo.size = header.getInt();

                if (codecBuffer.length < bufferInfo.size) {
                    Log.i(TAG, "codecBuffer.length:" + codecBuffer.length + " < bufferInfo.size:" + bufferInfo.size);
                    codecBuffer = new byte[bufferInfo.size];
                }

                recv(inputStream, codecBuffer, bufferInfo.size);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.i(TAG, "BUFFER_FLAG_CODEC_CONFIG");

                    recv(inputStream, headerBuffer, 8);

                    ByteBuffer sizeBuffer = ByteBuffer.wrap(headerBuffer, 0, 8);
                    int width = sizeBuffer.getInt();
                    int height = sizeBuffer.getInt();
                    Log.i(TAG, "sizeBuffer width:" + width);
                    Log.i(TAG, "sizeBuffer height:" + height);

                    if (mMediaDecoder != null) {
                        mMediaDecoder.stop();
                    }

                    ByteBuffer csd0 = ByteBuffer.wrap(codecBuffer, 0, bufferInfo.size);
                    mMediaDecoder.configure(width, height,  csd0, mSurface);

                    mMediaDecoder.start();
                } else {
                    mMediaDecoder.decode(codecBuffer, bufferInfo);
                }
            }
        }
    }

    private static void recv(InputStream inputStream, byte[] buffer, int sum) throws Exception {
        int read = 0;
        while (sum - read > 0) {
            int len = inputStream.read(buffer, read, sum - read);
            if (len == -1) {
                throw new RuntimeException("socket closed");
            }
            read += len;
        }
    }
}


