package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

public final class SecondaryDisplayLauncher {
    private static String TAG = "SecondaryDisplayLauncher";

    public static void startSelfSecondaryLauncher() {
        Intent secondaryHomeIntent = new Intent();
        secondaryHomeIntent.setClassName(Utils.APP_PACKAGE_NAME, Utils.APP_SECOND_ACTIVITY_CLASS_NAME);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(DisplayInfo.getDisplayId());
        try {
            @SuppressLint("BlockedPrivateApi")
            Method method = ActivityOptions.class.getDeclaredMethod("setLaunchActivityType", int.class);
            method.invoke(options, /* ACTIVITY_TYPE_HOME */ 2);
        } catch (Exception e) {
            Ln.w(TAG, "setLaunchActivityType failed", e);

            secondaryHomeIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }

        int ret = ServiceManager.getActivityManager().startActivity(secondaryHomeIntent, options.toBundle());
        Ln.i(TAG, "Start secondary launcher activity ret:" + ret);
        if (ret < 0) {
            Ln.e(TAG, "Could not start secondary launcher activity by ActivityManager");
            Utils.startActivity(Utils.APP_SECOND_ACTIVITY_NAME, DisplayInfo.getDisplayId());
        }
    }
}
