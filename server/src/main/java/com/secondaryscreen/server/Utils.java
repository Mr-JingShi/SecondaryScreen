package com.secondaryscreen.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Utils {
    private static String TAG = "Utils";
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
        String cmd = "am stack list | grep " + activity;
        Shell.Result sr = Shell.execCommand(cmd);
        if (sr.mResult == 0 && sr.mSuccessMsg != null && !sr.mSuccessMsg.isEmpty()) {
            return true;
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
        Shell.Result sr = Shell.execCommand(text);
        if (sr.mResult == 0) {
            System.out.println("am start secondActivity success");
        }
    }
}