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

    private static TaskStackListener mTaskStackListener;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void startSystemSecondaryLauncher() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo homeResolveInfo = (ResolveInfo) ServiceManager.getPackageManager().resolveActivity(
                    /* intent */ homeIntent,
                    /* flags */ android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                    /* userId */ /* UserHandle.USER_SYSTEM */ 0);

            Intent secondaryHomeIntent = new Intent(Intent.ACTION_MAIN);
            secondaryHomeIntent.addCategory(Intent.CATEGORY_SECONDARY_HOME);
            secondaryHomeIntent.addCategory(Intent.CATEGORY_DEFAULT);
            ResolveInfo secondaryHomeResolveInfo = (ResolveInfo) ServiceManager.getPackageManager().resolveActivity(
                    /* intent */ secondaryHomeIntent,
                    /* flags */ android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                    /* userId */ /* UserHandle.USER_SYSTEM */ 0);

            Ln.i(TAG, "homeResolveInfo:" + homeResolveInfo.activityInfo.packageName);
            Ln.i(TAG, "secondaryHomeResolveInfo:" + secondaryHomeResolveInfo.activityInfo.packageName);
            if (homeResolveInfo.activityInfo.packageName.equals(secondaryHomeResolveInfo.activityInfo.packageName)) {
                Ln.i(TAG, "Secondary launcher activity have not start, start secondary launcher activity");
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());
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
                    String activityName = secondaryHomeResolveInfo.activityInfo.packageName + "/" + secondaryHomeResolveInfo.activityInfo.name;
                    Utils.startActivity(activityName, DisplayInfo.getMirrorDisplayId());
                }
            }
        } catch (Exception e) {
            Ln.w(TAG, "SecondaryDisplayLauncher failed", e);
        }
    }

    public static void startSelfSecondaryLauncher() {
        Intent secondaryHomeIntent = new Intent();
        secondaryHomeIntent.setClassName(Utils.APP_PACKAGE_NAME, Utils.APP_SECOND_ACTIVITY_CLASS_NAME);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());
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
            Utils.startActivity(Utils.APP_SECOND_ACTIVITY_NAME, DisplayInfo.getMirrorDisplayId());
        }
    }

    public static void registerTaskStackListener() {
        ServiceManager.getActivityTaskManager().setDisplayToSingleTaskInstance(DisplayInfo.getMirrorDisplayId());

        mTaskStackListener = new TaskStackListener() {
            @Override
            public void onActivityLaunchOnSecondaryDisplayFailed(android.app.ActivityManager.RunningTaskInfo taskInfo,
                                                                 int requestedDisplayId) {
                Ln.d(TAG, "onActivityLaunchOnSecondaryDisplayFailed requestedDisplayId:" + requestedDisplayId);
            }

            @Override
            public void onActivityLaunchOnSecondaryDisplayRerouted(android.app.ActivityManager.RunningTaskInfo taskInfo,
                                                                   int requestedDisplayId) {
                Ln.d(TAG, "onActivityLaunchOnSecondaryDisplayRerouted requestedDisplayId:" + requestedDisplayId);

                ResolveInfo resolveInfo = (ResolveInfo) ServiceManager.getPackageManager().resolveActivity(taskInfo.baseIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY, 0);
                Ln.i(TAG, "resolveInfo packageName:" + resolveInfo.activityInfo.packageName);
                Ln.i(TAG, "resolveInfo exported:" + resolveInfo.activityInfo.exported);

                if (requestedDisplayId == DisplayInfo.getMirrorDisplayId()) {
                    if (resolveInfo.activityInfo.exported) {
                        ActivityOptions options = ActivityOptions.makeBasic();
                        options.setLaunchDisplayId(requestedDisplayId);
                        ServiceManager.getActivityManager().startActivity(taskInfo.baseIntent, options.toBundle());
                    }
                }
            }
        };

        ServiceManager.getActivityManager().registerTaskStackListener(mTaskStackListener);
    }

    public static void unregisterTaskStackListener() {
        if (mTaskStackListener != null) {
            ServiceManager.getActivityManager().unregisterTaskStackListener(mTaskStackListener);
        }
    }
}
