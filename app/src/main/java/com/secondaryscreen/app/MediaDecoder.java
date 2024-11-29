package com.secondaryscreen.app;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;


public class MediaDecoder {
    private static String TAG = "MediaDecoder";
    private static String VIDEO_FORMAT = "video/avc";
    private MediaCodec mMediaCodec;
    private ByteBuffer mCodecBuffer;
    private MediaCodec.BufferInfo mBufferInfo;
    private final AtomicBoolean mDecoderRunning;
    private final AtomicBoolean mDecoderReset;
    private Thread mDecoderThread;
    private Thread mSocketThread;
    private ArrayList<String> mCodecList = new ArrayList<>();

    public MediaDecoder() {
        mDecoderRunning = new AtomicBoolean(false);
        mDecoderReset = new AtomicBoolean(false);

        mDecoderThread = new MediaCodecThread();
        mDecoderThread.start();

        getDecodeList();
    }

    private void getDecodeList() {
        MediaCodecList codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo codecInfo : codecs.getCodecInfos()) {
            if (!codecInfo.isEncoder() && Arrays.asList(codecInfo.getSupportedTypes()).contains(VIDEO_FORMAT)) {
                Log.i(TAG, "decode name:" + codecInfo.getName());
                mCodecList.add(codecInfo.getName());
            }
        }
    }

    private MediaCodec createByCodecName(String codecName, MediaFormat format, Surface surface) throws IOException {
        MediaCodec codec = MediaCodec.createByCodecName(codecName);
        Log.d(TAG, "configure cache decoder:" + codecName);
        codec.configure(format, surface, null, 0);
        return codec;
    }

    public MediaCodec createDecoderByType(String type) throws IOException {
        return MediaCodec.createDecoderByType(type);
    }

    public void configure(int width, int height, ByteBuffer csd0, Surface surface) throws Exception {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_FORMAT, width, height);
        format.setByteBuffer("csd-0", csd0);

        String decoderCache = PrivatePreferences.getDecoder(width, height);
        if (decoderCache != null && !decoderCache.isEmpty()) {
            try {
                mMediaCodec = MediaCodec.createByCodecName(decoderCache);
                Log.i(TAG, "configure cache decoder:" + decoderCache);
                mMediaCodec.configure(format, surface, null, 0);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String decoderDefault = null;
        try {
            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_FORMAT);
            decoderDefault = mMediaCodec.getName();
            Log.i(TAG, "configure default decoder:" + decoderDefault);
            mMediaCodec.configure(format, surface, null, 0);
            PrivatePreferences.appendDecoderInfo(width, height, decoderDefault);
            return;
        } catch (Exception e) {
            e.printStackTrace();

            for (String decoder : mCodecList) {
                if (decoderCache != null && decoder.equals(decoderCache)) {
                    continue;
                }
                if (decoderDefault != null && decoder.equals(decoderDefault)) {
                    continue;
                }
                try {
                    mMediaCodec = MediaCodec.createByCodecName(decoder);
                    Log.i(TAG, "configure codecName:" + decoder);
                    mMediaCodec.configure(format, surface, null, 0);
                    PrivatePreferences.appendDecoderInfo(width, height, decoder);
                    return;
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
        throw new Exception("decoder configure failed");
    }

    public void start() {
        mMediaCodec.start();

        LockSupport.unpark(mDecoderThread);
    }

    public void reset(Thread thread) {
        if (mDecoderRunning.get()) {
            mSocketThread = thread;
            mDecoderReset.set(true);

            LockSupport.park();
        }
    }

    public void shutdown() {
        try {
            if (mDecoderThread != null
                && mDecoderThread.isAlive()
                && !mDecoderThread.isInterrupted()) {
                mDecoderThread.interrupt();
                mDecoderThread.join();
                mDecoderThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void decode(ByteBuffer codecBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (mCodecBuffer != null && mBufferInfo != null) {
            if (!tryDecode(mCodecBuffer, mBufferInfo)) {
                Log.w(TAG, "decode failed again");
            }
            mCodecBuffer = null;
            mBufferInfo = null;
        }
        if (!tryDecode(codecBuffer, bufferInfo)) {
            Log.w(TAG, "decode failed");
            mCodecBuffer = ByteBuffer.allocate(bufferInfo.size);
            mCodecBuffer.put(codecBuffer);

            mBufferInfo = new MediaCodec.BufferInfo();
            mBufferInfo.set(bufferInfo.offset, mBufferInfo.size, mBufferInfo.presentationTimeUs, mBufferInfo.flags);
        }
    }

    public boolean tryDecode(ByteBuffer codecBuffer, MediaCodec.BufferInfo bufferInfo) {
        /**
         * Returns the index of an input buffer to be filled with valid data
         * or -1 if no such buffer is currently available.
         * This method will return immediately if timeoutUs == 0, wait indefinitely
         * for the availability of an input buffer if timeoutUs &lt; 0 or wait up
         * to "timeoutUs" microseconds if timeoutUs &gt; 0.
         * @param timeoutUs The timeout in microseconds, a negative timeout indicates "infinite".
         * @throws IllegalStateException if not in the Executing state,
         *         or codec is configured in asynchronous mode.
         * @throws MediaCodec.CodecException upon codec error.
         */
        // public final int dequeueInputBuffer(long timeoutUs) {
        // dequeueInputBuffer有时返回-1，返回-1时如果把buffer丢掉，会导致花屏，这里最大尝试三次
        for (int i = 1; i <= 3 ; ++i) {
            int index = mMediaCodec.dequeueInputBuffer(i * 10000L);
            if (index >= 0) {
                ByteBuffer buffer = mMediaCodec.getInputBuffer(index);
                if (buffer != null) {
                    buffer.clear();
                    buffer.put(codecBuffer);
                    mMediaCodec.queueInputBuffer(index, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                }

                return true;
            }
        }
        return false;
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
                while (!Thread.currentThread().isInterrupted()) {
                    LockSupport.park();
                    mDecoderRunning.set(true);
                    while (!mDecoderReset.get()) {
                        int index = mMediaCodec.dequeueOutputBuffer(info, 10000L);
                        if (index >= 0) {
                            // setting true is telling system to render frame onto Surface
                            mMediaCodec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break;
                            }
                        }
                    }
                    mDecoderRunning.set(false);
                    mDecoderReset.set(false);
                    mMediaCodec.stop();
                    mMediaCodec.release();
                    mMediaCodec = null;
                    LockSupport.unpark(mSocketThread);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


