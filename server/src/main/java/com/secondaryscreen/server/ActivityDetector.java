package com.secondaryscreen.server;

import android.app.ActivityOptions;
import android.content.Intent;

public class ActivityDetector {
    private static String TAG = "ActivityDetector";
    private static int SCAN_INTERVAL = 1000; // 1s
    private static int WAIT_INTERVAL = 2000; // 2s
    private String mFirstActivity;
    private String mSecondActivity;
    private String mSecondActivityPackage;
    private String mSecondActivityClassName;
    private Thread mThread;

    public ActivityDetector(String firstActivity, String secondActivity) {
        this.mFirstActivity = firstActivity;
        this.mSecondActivity = secondActivity;
        this.mSecondActivityPackage = mSecondActivity.substring(0, mSecondActivity.indexOf("/"));
        this.mSecondActivityClassName = mSecondActivity.substring(mSecondActivity.indexOf("/") + 1);
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
        Intent intent = new Intent();
        intent.setClassName(mSecondActivityPackage, mSecondActivityClassName);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());

        int result = ServiceManager.getActivityManager().startActivity(intent, options.toBundle());
        Ln.i(TAG, "DetectorThread result:" + result);
        if (result < 0) {
            Ln.e(TAG, "Could not start second activity by ActivityManager");
            Utils.startActivity(mSecondActivity, DisplayInfo.getMirrorDisplayId());
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