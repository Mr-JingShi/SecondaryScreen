package com.secondaryscreen.server;

import android.os.Build;

public class Server {
    public static String TAG = "Server";
    public static void main(String[] args) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw new RuntimeException("暂不支持Android 10以下设备！！！");
            }
            if (args.length < 2) {
                throw new RuntimeException("必须指定activity！！！");
            }

            String firstActivity = Utils.prettifyActivity(args[0]);
            String secondActivity = Utils.prettifyActivity(args[1]);
            Ln.i(TAG, "firstActivity:" + firstActivity + " secondActivity:" + secondActivity);

            KillSelf.start();

            ActivityDetector activityDetector = new ActivityDetector(firstActivity, secondActivity);
            activityDetector.start();

            DisplayConnection displayConnection = new DisplayConnection(()->{
                activityDetector.startSecondActivity();
            });
            displayConnection.start();

            ControlConnection controlConnection = new ControlConnection();
            controlConnection.start();

            controlConnection.join();
            displayConnection.join();
            activityDetector.stop();
            Utils.shutdown();
        } catch (Exception e) {
            Ln.w(TAG, "Server main exception", e);
        }
    }
}