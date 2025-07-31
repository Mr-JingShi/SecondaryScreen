package com.secondaryscreen.app;

import android.content.SharedPreferences;

public class PrivatePreferences {
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notificationPermissionRequested";
    private static final String SERVICE_ADB_TSL_PORT = "service.adb.tls.port";
    private static final String START_APP_LIST = "startAppList";

    public static boolean getNotificationPermissionRequested() {
        return getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false);
    }
    public static void setNotificationPermissionRequested(boolean value) {
        putBoolean(NOTIFICATION_PERMISSION_REQUESTED, value);
    }
    public static int getServiceAdbTslPort() {
        return getInt(SERVICE_ADB_TSL_PORT, 0);
    }
    public static void setServiceAdbTslPort(int value) {
        putInt(SERVICE_ADB_TSL_PORT, value);
    }

    public static String getStartAppList() {
        return getString(START_APP_LIST, "");
    }
    public static void setStartAppList(String value) {
        putString(START_APP_LIST, value);
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