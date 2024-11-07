package com.secondaryscreen.server;

public class Utils {
    private static String TAG = "Utils";
    public static final String PACKAGE_NAME = "com.android.shell";
    public static int CONTROL_CHANNEL_PORT = 8402;
    public static int VIDEO_CHANNEL_PORT = 8403;
    public static int DISPLAY_CHANNEL_PORT = 8404;
    private static boolean mIsSingleMachineMode = true;

    static boolean isSingleMachineMode() {
        return mIsSingleMachineMode;
    }
    static void setSingleMachineMode(boolean isSingleMachineMode) {
        synchronized (Utils.class) {
            mIsSingleMachineMode = isSingleMachineMode;
        }
    }

    static boolean activityRunning(String activity) {
        try {
            String cmd = "am stack list | grep " + activity + " | wc -l";
            String result = Shell.execReadOutput("sh", "-c", cmd);
            int count = Integer.parseInt(result.trim());
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static void startActivity(String activity, int displayId) {
        StringBuilder sb = new StringBuilder();

        sb.append("am start -n ");
        sb.append(activity);
        sb.append(" --display ");
        sb.append(displayId);

        String text = sb.toString();
        try {
            Shell.exec("sh", "-c", text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (IllegalArgumentException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static String prettifyActivity(String activity) {
        int index = activity.indexOf ("/.");
        if (index > 0) {
            String prefix = activity.substring(0, index);
            activity = activity.replace("/", "/" + prefix);
        }
        return activity;
    }
}