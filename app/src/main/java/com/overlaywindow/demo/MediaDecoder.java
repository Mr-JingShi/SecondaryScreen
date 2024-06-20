package com.overlaywindow.demo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;


public class MediaDecoder {
    private static String TAG = "MediaDecoder";
    private static String VIDEO_FORMAT = "video/avc";
    private Thread mThread;
    private MediaCodec mMediaCodec;
    private volatile boolean mRunning = false;

    public MediaDecoder() {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_FORMAT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void configure(int width, int height, ByteBuffer csd0, Surface surface) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_FORMAT, width, height);

        format.setByteBuffer("csd-0", csd0);

        mMediaCodec.configure(format, surface, null, 0);
    }

    public void start() {
        mMediaCodec.start();

        mRunning = true;
        mThread = new MediaCodecThread();
        mThread.start();
    }

    public void stop() {
        mRunning = false;
    }

    public void decode(byte[] codecBuffer, MediaCodec.BufferInfo bufferInfo) {
        int index = mMediaCodec.dequeueInputBuffer(10000L);
        if (index >= 0) {
            ByteBuffer buffer = mMediaCodec.getInputBuffer(index);
            if (buffer != null) {
                buffer.clear();
                buffer.put(codecBuffer, bufferInfo.offset, bufferInfo.size);
                mMediaCodec.queueInputBuffer(index, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
            }
        }
    }

    class MediaCodecThread extends Thread {
        public MediaCodecThread() {
            super("MediaCodecThread");
            Log.i(TAG, "MediaCodecThread");
        }
        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mRunning) {
                    int index = mMediaCodec.dequeueOutputBuffer(info, 10000L);
                    if (index >= 0) {
                        // setting true is telling system to render frame onto Surface
                        mMediaCodec.releaseOutputBuffer(index, true);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            break;
                        }
                    }
                }
            } finally {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
        }
    }
}


