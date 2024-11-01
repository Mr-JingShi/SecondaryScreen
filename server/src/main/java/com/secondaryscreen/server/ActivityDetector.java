package com.secondaryscreen.server;

public class ActivityDetector {
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
            System.out.println("DetectorThread");
        }
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isReady()) {
                        Thread.sleep(WAIT_INTERVAL);

                        if (isReady()) {
                            startSecondActivity();
                        }
                    }

                    Thread.sleep(SCAN_INTERVAL);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("detector exception：" + e);
                }
            }
        }
    }
}