package com.secondaryscreen.server;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

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

    static boolean activityRunning(@NonNull String activity) {
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

    static void startActivity(@NonNull String activity, int displayId) {
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

    static String prettifyActivity(@NonNull String activity) {
        int index = activity.indexOf ("/.");
        if (index > 0) {
            String prefix = activity.substring(0, index);
            activity = activity.replace("/", "/" + prefix);
        }
        return activity;
    }

    static Class<?> findClass(Class<?>[] innerClasses, String name) throws ClassNotFoundException {
        for (Class<?> clazz : innerClasses) {
            if (clazz.getName().equals(name)) {
                return clazz;
            }
        }
        throw new ClassNotFoundException(name);
    }

    static Method findMethodAndMakeAccessible(Method[] methods, String name) throws NoSuchMethodException {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }
}