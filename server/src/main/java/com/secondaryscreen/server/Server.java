package com.secondaryscreen.server;

import android.os.Build;

public class Server {
    public static String TAG = "Server";
    public static void main(String[] args) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw new RuntimeException("暂不支持Android 10以下设备！！！");
            }

            String firstActivity = null;
            String secondActivity = null;

            if (args.length >= 2) {
                firstActivity = Utils.prettifyActivity(args[0]);
                secondActivity = Utils.prettifyActivity(args[1]);
                Ln.i(TAG, "firstActivity:" + firstActivity + " secondActivity:" + secondActivity);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    throw new RuntimeException(">= Android 13时，无需指定activity！！！");
                }
            }

            DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(true);

            Size size = displayInfo.getSize();
            int width = size.getWidth();
            int height = size.getHeight();
            int rotation = displayInfo.getRotation();
            int densityDpi = displayInfo.getDensityDpi();
            if (rotation % 2 == 1) {
                int temp = width;
                width = height;
                height = temp;
            }

            int displayId = SurfaceControl.createVirtualDisplay(width, height, densityDpi);

            Ln.i(TAG, "displayId:" + displayId);

            DisplayInfo.setMirrorDisplayId(displayId);

            ServiceManager.getWindowManager().freezeRotation(displayId, rotation);

            ActivityDetector activityDetector =  null;
            if (firstActivity != null && secondActivity != null) {
                activityDetector = new ActivityDetector(firstActivity, secondActivity);
                activityDetector.start();
            }

            DisplayConnection displayConnection = new DisplayConnection(width, height, rotation, densityDpi);
            displayConnection.start();

            ControlConnection controlConnection = new ControlConnection();
            controlConnection.start();

            VideoConnection videoConnection = new VideoConnection();
            videoConnection.start();

            videoConnection.join();
            controlConnection.join();
            displayConnection.join();
            if (activityDetector != null) {
                activityDetector.join();
            }
        } catch (Exception e) {
            Ln.w(TAG, "Server main exception", e);
        }
    }
}