package com.overlaywindow.demo;

import android.content.SharedPreferences;

public class PrivatePreferences {
    private static SharedPreferences getSharedPreferences() {
        return Utils.getContext().getSharedPreferences("SecondaryScreen", android.content.Context.MODE_PRIVATE);
    }

    public static String getString(String key, String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    public static void putString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
    }

    public static void putBoolean(String key, Boolean value) {
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }
}