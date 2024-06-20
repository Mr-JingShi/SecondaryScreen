package com.overlaywindow.server;

public class Server {
    public static void main(String[] args) {
        try {
            String firstActivity = null;
            String secondActivity = null;

            if (args.length == 2) {
                firstActivity = args[0];
                secondActivity = args[1];
                System.out.println("firstActivity:" + firstActivity);
                System.out.println("secondActivity:" + secondActivity);
            }

            DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(0);

            int displayId = SurfaceControl.createVirtualDisplay(displayInfo.getSize().getWidth(), displayInfo.getSize().getHeight(), displayInfo.getDensityDpi());

            System.out.println("displayId:" + displayId);

            displayInfo.setMirrorDisplayId(displayId);

            ActivityDetector activityDetector =  null;
            if (firstActivity != null && secondActivity != null) {
                activityDetector = new ActivityDetector(firstActivity, secondActivity, displayId);
                activityDetector.start();
            }

            ControlConnection controlConnection = new ControlConnection(displayId);
            controlConnection.start();

            VideoConnection videoConnection = new VideoConnection(displayInfo);
            videoConnection.start();

            controlConnection.join();
            videoConnection.join();
            if (activityDetector != null) {
                activityDetector.join();
            }
        } catch (Exception e) {
            System.out.println("listen Exception:" + e);
        }
    }
}