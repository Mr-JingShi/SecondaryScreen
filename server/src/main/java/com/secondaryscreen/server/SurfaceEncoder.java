package com.secondaryscreen.server;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/SurfaceEncoder.java

public class SurfaceEncoder {
    private static final String TAG = "SurfaceEncoder";
    private static final int DEFAULT_I_FRAME_INTERVAL = 10; // seconds
    private static final int REPEAT_FRAME_DELAY_US = 100_000; // repeat after 100ms
    private static final String KEY_MAX_FPS_TO_ENCODER = "max-fps-to-encoder";
    private static String VIDEO_FORMAT = "video/avc";

    // Keep the values in descending order
    private static final int[] MAX_SIZE_FALLBACK = {2560, 1920, 1600, 1280, 1024, 800};
    private static final int MAX_CONSECUTIVE_ERRORS = 3;
    private final int mVideoBitRate;
    private final int mMaxFps;
    private final boolean mDownsizeOnError;
    private boolean mFirstFrameSent = false;
    private int mConsecutiveErrors;

    public SurfaceEncoder(int videoBitRate, int maxFps, boolean downsizeOnError) {
        this.mVideoBitRate = videoBitRate;
        this.mMaxFps = maxFps;
        this.mDownsizeOnError = downsizeOnError;
    }

    public void streamScreen() {
        try {
            ScreenCapture screenCapture = new ScreenCapture();

            Streamer streamer = new Streamer();

            MediaFormat format = createFormat(VIDEO_FORMAT, mVideoBitRate, mMaxFps);

            while (!Thread.currentThread().isInterrupted()) {
                String remoteAddress = screenCapture.getRemoteAddress();
                Ln.d(TAG, "streamScreen remoteAddress:" + remoteAddress);

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(remoteAddress, Utils.VIDEO_CHANNEL_PORT), 3000);
                    Ln.d(TAG, "VideoChannel socket connect success");

                    screenCapture.computeScreenInfo();
                    streamer.setSocket(socket);

                    streamScreen(screenCapture, streamer, format);
                } catch (Exception e) {
                    Ln.w(TAG, "streamScreen exception", e);
                }
            }

            screenCapture.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void streamScreen(ScreenCapture screenCapture, Streamer streamer, MediaFormat format) throws Exception {
        boolean alive;

        MediaCodec mediaCodec = createMediaCodec(null);
        do {
            mFirstFrameSent = false;
            Size size = screenCapture.getSize();
            Ln.d(TAG, "streamScreen size width:" + size.getWidth() + " height:" + size.getHeight());
            format.setInteger(MediaFormat.KEY_WIDTH, size.getWidth());
            format.setInteger(MediaFormat.KEY_HEIGHT, size.getHeight());

            Surface surface = null;
            try {
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                surface = mediaCodec.createInputSurface();

                screenCapture.start(surface);

                mediaCodec.start();

                alive = encode(screenCapture, streamer, mediaCodec);
                // do not call stop() on exception, it would trigger an IllegalStateException
                mediaCodec.stop();
            } catch (IllegalStateException | IllegalArgumentException e) {
                Ln.w(TAG, "Encoding error", e);
                if (!prepareRetry(screenCapture, size)) {
                    throw e;
                }
                Ln.w(TAG, "Retrying...");
                alive = true;
            } finally {
                mediaCodec.reset();
                if (surface != null) {
                    surface.release();
                }
            }
        } while (alive);
        mediaCodec.release();
    }

    private boolean prepareRetry(ScreenCapture screenCapture, Size currentSize) {
        if (mFirstFrameSent) {
            ++mConsecutiveErrors;
            if (mConsecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                // Definitively fail
                return false;
            }

            // Wait a bit to increase the probability that retrying will fix the problem
            SystemClock.sleep(50);
            return true;
        }

        if (!mDownsizeOnError) {
            // Must fail immediately
            return false;
        }

        // Downsizing on error is only enabled if an encoding failure occurs before the first frame (downsizing later could be surprising)

        int newMaxSize = chooseMaxSizeFallback(currentSize);
        if (newMaxSize == 0) {
            // Must definitively fail
            return false;
        }

        boolean accepted = screenCapture.setMaxSize(newMaxSize);
        if (!accepted) {
            return false;
        }

        // Retry with a smaller size
        Ln.i(TAG, "Retrying with -m" + newMaxSize + "...");
        return true;
    }

    private static int chooseMaxSizeFallback(Size failedSize) {
        int currentMaxSize = Math.max(failedSize.getWidth(), failedSize.getHeight());
        for (int value : MAX_SIZE_FALLBACK) {
            if (value < currentMaxSize) {
                // We found a smaller value to reduce the video size
                return value;
            }
        }
        // No fallback, fail definitively
        return 0;
    }

    private boolean encode(ScreenCapture screenCapture, Streamer streamer, MediaCodec codec) throws IOException {
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        screenCapture.consumeReset();
        screenCapture.restartReset();

        while (!screenCapture.consumeReset() && !eof) {
            if (screenCapture.restartReset()) {
                eof = true;
                break;
            }
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000L);

            if (screenCapture.restartReset()) {
                eof = true;
                break;
            }
            if (screenCapture.consumeReset()) {
                // must restart encoding with new size
                break;
            }

            try {
                if (outputBufferId >= 0) {
                    eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                    ByteBuffer codecBuffer = codec.getOutputBuffer(outputBufferId);

                    streamer.writePacket(codecBuffer, bufferInfo);

                    boolean isConfig = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                    if (!isConfig) {
                        // If this is not a config packet, then it contains a frame
                        mFirstFrameSent = true;
                        mConsecutiveErrors = 0;
                    } else {
                        streamer.writeVideoHeader(screenCapture.getSize());
                    }
                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }

        return !eof;
    }

    private static MediaCodec createMediaCodec(String encoderName) throws IOException, IllegalArgumentException {
        if (encoderName != null && !encoderName.isEmpty()) {
            try {
                return MediaCodec.createByCodecName(encoderName);
            } catch (Exception e) {
                Ln.w("Could not create video encoder by name ", encoderName);
            }
        }

        try {
            MediaCodec mediaCodec = MediaCodec.createEncoderByType(VIDEO_FORMAT);
            Ln.i(TAG, "Using video encoder:" + mediaCodec.getName());
            return mediaCodec;
        } catch (IOException | IllegalArgumentException e) {
            Ln.w(TAG, "Could not create video encoder for " + VIDEO_FORMAT);
            throw e;
        }
    }

    private static void getEncoders() {
        MediaCodecList codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo codecInfo : codecs.getCodecInfos()) {
            if (codecInfo.isEncoder() && Arrays.asList(codecInfo.getSupportedTypes()).contains(VIDEO_FORMAT)) {
                Log.i(TAG, "decode name:" + codecInfo.getName());
            }
        }
    }

    private static MediaFormat createFormat(String videoMimeType, int bitRate, int maxFps) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, videoMimeType);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        // must be present to configure the encoder, but does not impact the actual frame rate, which is variable
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL);
        // display the very first frame, and recover from bad quality when no new frames
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, REPEAT_FRAME_DELAY_US); // µs
        if (maxFps > 0) {
            // The key existed privately before Android 10:
            // <https://android.googlesource.com/platform/frameworks/base/+/625f0aad9f7a259b6881006ad8710adce57d1384%5E%21/>
            // <https://github.com/Genymobile/scrcpy/issues/488#issuecomment-567321437>
            format.setFloat(KEY_MAX_FPS_TO_ENCODER, maxFps);
        }

        return format;
    }

    public static int chooseMaxSize(Size size) {
        int maxSize = 0;
        try {
            MediaCodec mediaCodec = MediaCodec.createDecoderByType(VIDEO_FORMAT);
            int width = size.getWidth();
            int height = size.getHeight();
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(VIDEO_FORMAT, width, height);
            mediaCodec.configure(mediaFormat, null, null, 0);

            MediaFormat outputFormat = mediaCodec.getOutputFormat();
            Ln.d(TAG, "outputFormat:" + outputFormat);
            if (outputFormat != null) {
                int maxWidth = outputFormat.getInteger(MediaFormat.KEY_MAX_WIDTH, 0);
                int maxHeight = outputFormat.getInteger(MediaFormat.KEY_MAX_HEIGHT, 0);
                if ((width > height && maxWidth < maxHeight)
                        || (width < height && maxWidth > maxHeight)) {
                    int tmp = maxWidth;
                    maxWidth = maxHeight;
                    maxHeight = tmp;
                }
                Ln.d(TAG, "KEY_MAX_WIDTH:" + maxWidth + " KEY_MAX_HEIGHT:" + maxHeight);

                if (maxWidth != 0 && maxHeight != 0) {
                    for (int i = 0; i < MAX_CONSECUTIVE_ERRORS; ++i) {
                        Ln.d(TAG, "size.width:" + size.getWidth() + ", size.height:" + size.getHeight());

                        if (maxWidth < size.getWidth() || maxHeight < size.getHeight()) {
                            maxSize = chooseMaxSizeFallback(size);
                            if (maxSize == 0) {
                                break;
                            }
                        } else {
                            break;
                        }

                        size = ScreenInfo.computeVideoSize(size.getWidth(), size.getHeight(), maxSize);
                    }
                }
            }
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            Ln.w(TAG, "chooseMaxSize IOException", e);
        }

        return maxSize;
    }
}
