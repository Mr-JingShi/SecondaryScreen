package com.secondaryscreen.server;

import android.os.Build;
import android.view.IRotationWatcher;

public class Server {
    public static void main(String[] args) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw new RuntimeException("暂不支持Android 10以下设备！！！");
            }

            String firstActivity = null;
            String secondActivity = null;

            if (args.length >= 2) {
                firstActivity = args[0];
                secondActivity = args[1];
                System.out.println("firstActivity:" + firstActivity);
                System.out.println("secondActivity:" + secondActivity);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    throw new RuntimeException(">= Android 13时，无需指定activity！！！");
                }
            }

            DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(true);

            int displayId = SurfaceControl.createVirtualDisplay(displayInfo);

            System.out.println("displayId:" + displayId);

            DisplayInfo.setMirrorDisplayId(displayId);

            WindowManager windowManager = ServiceManager.getWindowManager();
            windowManager.freezeRotation(displayId, displayInfo.getRotation());

            windowManager.registerRotationWatcher(new IRotationWatcher.Stub() {
                @Override
                public void onRotationChanged(int rotation) {
                System.out.println("onRotationChanged rotation:" + rotation);

                if (!windowManager.isRotationFrozen(displayId)) {
                    windowManager.thawRotation(displayId);
                }

                windowManager.freezeRotation(displayId, rotation);

                ServiceManager.getDisplayManager().getDisplayInfo(true);
                windowManager.onRotationChanged(rotation);
                }
            }, 0);

            ActivityDetector activityDetector =  null;
            if (firstActivity != null && secondActivity != null) {
                activityDetector = new ActivityDetector(firstActivity, secondActivity, displayId);
                activityDetector.start();
            }

            ControlConnection controlConnection = new ControlConnection(displayId);
            controlConnection.start();

            VideoConnection videoConnection = new VideoConnection();
            videoConnection.start();

            controlConnection.join();
            videoConnection.join();
            if (activityDetector != null) {
                activityDetector.join();
            }
        } catch (Exception e) {
            System.out.println("Server main exception:" + e);
        }
    }
}