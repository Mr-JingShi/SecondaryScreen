package com.secondaryscreen.server;

import java.io.InputStream;

public class Utils {
    private static String TAG = "Utils";
    private static boolean mIsSingleMachineMode = true;

    static boolean isSingleMachineMode() {
        return mIsSingleMachineMode;
    }
    static void setSingleMachineMode(boolean isSingleMachineMode) {
        synchronized (Utils.class) {
            mIsSingleMachineMode = isSingleMachineMode;
        }
    }

    static void recv(InputStream inputStream, byte[] buffer, int sum) throws Exception {
        int read = 0;
        while (sum - read > 0) {
            int len = inputStream.read(buffer, read, sum - read);
            if (len == -1) {
                throw new RuntimeException("socket closed");
            }
            read += len;
        }
    }

    static int byte4ToInt(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
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