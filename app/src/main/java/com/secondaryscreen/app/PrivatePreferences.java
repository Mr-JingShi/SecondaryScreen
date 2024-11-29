package com.secondaryscreen.app;

import android.content.SharedPreferences;

public class PrivatePreferences {
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notificationPermissionRequested";
    private static final String REMOTE_HOST = "remoteHost";
    private static final String RESOLUTION = "resolution";
    private static final String SERVICE_ADB_TSL_PORT = "service.adb.tls.port";
    private static final String DECODER_INFO = "decoderInfo";

    public static boolean getNotificationPermissionRequested() {
        return getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false);
    }
    public static void setNotificationPermissionRequested(boolean value) {
        putBoolean(NOTIFICATION_PERMISSION_REQUESTED, value);
    }
    public static String getRemoteHost() {
        return getString(REMOTE_HOST, "");
    }
    public static void setRemoteHost(String value) {
        putString(REMOTE_HOST, value);
    }
    public static String getResolution() {
        return getString(RESOLUTION, "");
    }
    public static void setResolution(String value) {
        putString(RESOLUTION, value);
    }
    public static int getServiceAdbTslPort() {
        return getInt(SERVICE_ADB_TSL_PORT, 0);
    }
    public static void setServiceAdbTslPort(int value) {
        putInt(SERVICE_ADB_TSL_PORT, value);
    }
    public static String getDecoder(int width, int height) {
        String decoderInfo = getString(DECODER_INFO, "");
        String[] split = decoderInfo.split("\n");
        for (String s : split) {
            String[] s1 = s.split("/");
            if (s1.length == 2 && s1[0].equals(width + "x" + height)) {
                return s1[1];
            }
        }
        return null;
    }
    public static void appendDecoderInfo(int width, int height, String decoder) {
        String decoderInfo = getString(DECODER_INFO, "");

        StringBuilder sb = new StringBuilder();
        if (!decoderInfo.isEmpty()) {
            sb.append(decoderInfo);
            sb.append("\n");
        }
        sb.append(width);
        sb.append("x");
        sb.append(height);
        sb.append("/");
        sb.append(decoder);
        putString(DECODER_INFO, sb.toString());
    }

    private static SharedPreferences getSharedPreferences() {
        return Utils.getContext().getSharedPreferences("SecondaryScreen", android.content.Context.MODE_PRIVATE);
    }

    private static String getString(String key, String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    private static int getInt(String key, int defaultValue) {
        return getSharedPreferences().getInt(key, defaultValue);
    }

    private static void putString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
    }

    private static void putBoolean(String key, Boolean value) {
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }

    private static void putInt(String key, int value) {
        getSharedPreferences().edit().putInt(key, value).apply();
    }
}