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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    throw new RuntimeException("大于等于Android 13时，不需要指定activity！！！");
                }
                firstActivity = Utils.prettifyActivity(args[0]);
                secondActivity = Utils.prettifyActivity(args[1]);
                Ln.i(TAG, "firstActivity:" + firstActivity + " secondActivity:" + secondActivity);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    throw new RuntimeException("小于Android 13时，必须指定activity！！！");
                }
            }

            KillSelf.start();

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 自定义virtualdisplay可以设置FLAG_TRUSTED参数，因此可以显示IME和系统装饰
                if (!ServiceManager.getWindowManager().shouldShowIme(displayId)) {
                    ServiceManager.getWindowManager().setShouldShowIme(displayId, true);
                }
                if (!ServiceManager.getWindowManager().shouldShowSystemDecors(displayId)) {
                    ServiceManager.getWindowManager().setShouldShowSystemDecors(displayId, true);
                }
            }

            ServiceManager.getWindowManager().freezeRotation(displayId, rotation);

            ActivityDetector activityDetector =  null;
            if (firstActivity != null && secondActivity != null) {
                activityDetector = new ActivityDetector(firstActivity, secondActivity);
                activityDetector.start();
            }

            DisplayConnection displayConnection = new DisplayConnection(width, height, densityDpi, rotation);
            displayConnection.start();

            ControlConnection controlConnection = new ControlConnection();
            controlConnection.start();

            VideoConnection videoConnection = new VideoConnection();
            videoConnection.start();

            videoConnection.join();
            controlConnection.join();
            displayConnection.join();
            if (activityDetector != null) {
                activityDetector.stop();
            }
            Utils.shutdown();
        } catch (Exception e) {
            Ln.w(TAG, "Server main exception", e);
        }
    }
}