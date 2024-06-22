package com.secondaryscreen.server;

public class ActivityDetector {
    private static int SCAN_INTERVAL = 1000; // 1s
    private static int WAIT_INTERVAL = 2000; // 2s
    private int mDisplayId = 0;
    private String mFirstActivity;
    private String mSecondActivity;
    private Thread mThread;

    public ActivityDetector(String firstActivity, String secondActivity, int displayId) {
        this.mFirstActivity = firstActivity;
        this.mSecondActivity = secondActivity;
        this.mDisplayId = displayId;
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

    private static boolean activityRunning(String activity) {
        String cmd = "am stack list | grep " + activity;
        Shell.Result sr = Shell.execCommand(cmd);
        if (sr.mResult == 0 && sr.mSuccessMsg != null && !sr.mSuccessMsg.isEmpty()) {
            return true;
        }
        return false;
    }

    private static void startActivity(String activity, int displayId) {
        StringBuilder sb = new StringBuilder();

        sb.append("am start -n ");
        sb.append(activity);
        sb.append(" --display ");
        sb.append(displayId);

        String text = sb.toString();
        Shell.Result sr = Shell.execCommand(text);
        if (sr.mResult == 0) {
            System.out.println("am start secondActivity success");
        }
    }

    private boolean isReady() {
        return activityRunning(mFirstActivity) && !activityRunning(mSecondActivity);
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
                            startActivity(mSecondActivity, mDisplayId);
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