package com.secondaryscreen.server;

import android.os.Looper;

public final class VideoConnection {
    private static String TAG = "VideoConnection";
    private Thread mThread;
    public VideoConnection() {
        mThread = new VideoChannelThread();
    }

    public void start() {
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    private class VideoChannelThread extends Thread {
        public VideoChannelThread() {
            super("VideoChannelThread");
            Ln.d(TAG, "VideoChannelThread");
        }

        @Override
        public void run() {
            // Some devices (Meizu) deadlock if the video encoding thread has no Looper
            // <https://github.com/Genymobile/scrcpy/issues/4143>
            Looper.prepare();

            SurfaceEncoder surfaceEncoder = new SurfaceEncoder(8000000, 0, true);
            surfaceEncoder.streamScreen();
        }
    }
}
