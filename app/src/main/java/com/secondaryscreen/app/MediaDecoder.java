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
    private MediaCodec mMediaCodec;
    private ByteBuffer mCodecBuffer;
    private MediaCodec.BufferInfo mBufferInfo;
    private final AtomicBoolean mDecoderRunning;
    private final AtomicBoolean mDecoderReset;
    private Thread mDecoderThread;
    private Thread mSocketThread;
    private ArrayList<String> mCodecList;

    public MediaDecoder() {
        mDecoderRunning = new AtomicBoolean(false);
        mDecoderReset = new AtomicBoolean(false);

        mDecoderThread = new MediaCodecThread();
        mDecoderThread.start();

        mCodecList = Utils.getDecodeList();
    }

    public void configure(int width, int height, ByteBuffer csd0, Surface surface) throws Exception {
        MediaFormat format = MediaFormat.createVideoFormat(Utils.VIDEO_FORMAT, width, height);
        format.setByteBuffer("csd-0", csd0);

        String decoderCache = PrivatePreferences.getDecoder(width, height);
        if (decoderCache != null && !decoderCache.isEmpty()) {
            try {
                mMediaCodec = MediaCodec.createByCodecName(decoderCache);
                Log.i(TAG, "configure cache decoder:" + decoderCache);
                mMediaCodec.configure(format, surface, null, 0);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /** remark
         *  华为MatePad SE设备--分辨率1200x200/320--HarmonyOS 3.0.0版本
         *  server侧 OMX.qcom.video.encoder.avc为默认编码器
         *  1200x2000 configure失败
         *  1152x1920 configure失败
         *  960x1600 configure成功
         *  然而app侧 OMX.qcom.video.decoder.avc为默认解码器
         *  960x1600 configure失败，报错如下：
         *  [OMX.qcom.video.decoder.avc] configureCodec returning error -12
         *  signalError(omxError 0x80001001, internalError -12)
         *  Codec reported err 0xfffffff4, actionCode 0, while in state 3/CONFIGURING
         *
         *  android.media.MediaCodec$CodecException: Error 0xfffffff4
         *      at android.media.MediaCodec.native_configure(Native Method)
         *      at android.media.MediaCodec.configure(MediaCodec.java:2176)
         *      at android.media.MediaCodec.configure(MediaCodec.java:2092)
         *      at com.secondaryscreen.app.MediaDecoder.configure(MediaDecoder.java:64)
         * 此时切换解码器为c2.android.avc.decoder则可configure成功
         */
        String decoderDefault = null;
        try {
            mMediaCodec = MediaCodec.createDecoderByType(Utils.VIDEO_FORMAT);
            decoderDefault = mMediaCodec.getName();
            Log.i(TAG, "configure default decoder:" + decoderDefault);
            mMediaCodec.configure(format, surface, null, 0);
            PrivatePreferences.appendDecoderInfo(width, height, decoderDefault);
            return;
        } catch (Exception e) {
            e.printStackTrace();

            for (String decoder : mCodecList) {
                if ((decoderCache != null && decoder.equals(decoderCache))
                    || (decoderDefault != null && decoder.equals(decoderDefault))) {
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


