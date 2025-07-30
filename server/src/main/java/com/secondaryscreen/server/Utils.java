package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.content.Intent;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static String TAG = "Utils";
    static final String APP_PACKAGE_NAME = "com.secondaryscreen.app";
    static final String APP_MAIN_ACTIVITY_CLASS_NAME = APP_PACKAGE_NAME + ".MainActivity";
    static final String APP_MAIN_ACTIVITY_NAME = APP_PACKAGE_NAME + "/" + APP_MAIN_ACTIVITY_CLASS_NAME;
    static final String APP_SECOND_ACTIVITY_CLASS_NAME = APP_PACKAGE_NAME + ".SecondActivity";
    static final String APP_SECOND_ACTIVITY_NAME = APP_PACKAGE_NAME + "/" + APP_SECOND_ACTIVITY_CLASS_NAME;
    static final String PACKAGE_NAME = "com.android.shell";
    static final String VIRTUALDISPLAY_NAME = "secondaryscreen";
    static int CONTROL_CHANNEL_PORT = 8402;
    static int DISPLAY_CHANNEL_PORT = 8404;
    private static ScheduledExecutorService mExecutor  = Executors.newSingleThreadScheduledExecutor();

    static void startActivity(@NonNull String activity, int displayId) {
        StringBuilder sb = new StringBuilder();

        sb.append("am start -n ");
        sb.append(activity);
        sb.append(" --display ");
        sb.append(displayId);

        String text = sb.toString();
        Ln.i(TAG, "startActivity:" + text);
        try {
            Shell.exec("sh", "-c", text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void startActivity(@NonNull String packageName, @NonNull String className, int displayId) {
        Ln.i(TAG, "startActivity packageName:" + packageName + " className:" + className + " displayId:" + displayId);
        if (ServiceManager.getDisplayManager().isActive(displayId)) {
            Intent intent = new Intent();
            intent.setClassName(packageName, className);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);

            int result = ServiceManager.getActivityManager().startActivity(intent, options.toBundle());
            Ln.i(TAG, "startActivity result:" + result);
            if (result != 0) {
                Ln.e(TAG, "Could not start activity by ActivityManager");
                String activityName = packageName + "/" + className;
                Utils.startActivity(activityName, displayId);
            }
        }
    }

    public static int byte4ToInt(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    static void schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        mExecutor.schedule(runnable, time, timeUnit);
    }

    static void shutdown() {
        mExecutor.shutdown();
    }

}