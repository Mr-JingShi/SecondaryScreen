package com.secondaryscreen.server;

public class ActivityDetector {
    private static String TAG = "ActivityDetector";
    private static int SCAN_INTERVAL = 1000; // 1s
    private static int WAIT_INTERVAL = 2000; // 2s
    private String mFirstActivity;
    private String mSecondActivity;
    private Thread mThread;

    public ActivityDetector(String firstActivity, String secondActivity) {
        this.mFirstActivity = firstActivity;
        this.mSecondActivity = secondActivity;
    }

    public void start() {
        mThread = new DetectorThread();
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    private void startSecondActivity() {
        int displayId = DisplayInfo.getMirrorDisplayId();
        if (displayId > 0) {
            Utils.startActivity(mSecondActivity, displayId);
        }
    }

    private boolean isReady() {
        return Utils.activityRunning(mFirstActivity) && !Utils.activityRunning(mSecondActivity);
    }

    class DetectorThread extends Thread {
        DetectorThread() {
            super("DetectorThread");
            Ln.i(TAG, "DetectorThread");
        }
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (isReady()) {
                    Utils.sleep(WAIT_INTERVAL);

                    if (isReady()) {
                        startSecondActivity();
                    }
                }

                Utils.sleep(SCAN_INTERVAL);
            }
        }
    }
}