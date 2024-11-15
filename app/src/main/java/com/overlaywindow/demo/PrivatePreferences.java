package com.overlaywindow.demo;

import android.content.SharedPreferences;

public class PrivatePreferences {
    public static final String NOTIFICATION_PERMISSION_REQUESTED = "notificationPermissionRequested";
    public static final String REMOTE_HOST = "remoteHost";
    public static final String RESOLUTION = "resolution";

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

    private static SharedPreferences getSharedPreferences() {
        return Utils.getContext().getSharedPreferences("SecondaryScreen", android.content.Context.MODE_PRIVATE);
    }

    private static String getString(String key, String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    private static void putString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
    }

    private static void putBoolean(String key, Boolean value) {
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }
}