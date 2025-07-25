package com.secondaryscreen.server;

import android.app.ActivityOptions;
import android.app.IActivityController;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ActivityDetector {
    private static String TAG = "ActivityDetector";
    @NonNull
    private String mTargetFirstActivity;
    @NonNull
    private String mTargetSecondActivity;
    @NonNull
    private String mTargetSecondActivityPackageName;
    @NonNull
    private String mTargetSecondActivityClassName;

    public ActivityDetector(String firstActivity, String secondActivity) {
        this.mTargetFirstActivity = firstActivity;
        this.mTargetSecondActivity = secondActivity;
        this.mTargetSecondActivityPackageName = mTargetSecondActivity.substring(0, mTargetSecondActivity.indexOf("/"));
        this.mTargetSecondActivityClassName = mTargetSecondActivity.substring(mTargetSecondActivity.indexOf("/") + 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void start() {
        ServiceManager.getActivityManager().setActivityController(new IActivityController.Stub() {
            @Override
            public boolean activityStarting(Intent intent, String pkg) {
                if (intent.getComponent() != null) {
                    String packageName = intent.getComponent().getPackageName();
                    String className = intent.getComponent().getClassName();
                    String activityName = packageName + "/" + className;
                    Ln.i(TAG, "activityStarting activityName:" + activityName);
                    if (ServiceManager.getDisplayManager().isActive(DisplayInfo.getDisplayId()) && mTargetFirstActivity.equals(activityName)) {
                        Utils.schedule(() -> {
                            startActivity(mTargetSecondActivityPackageName, mTargetSecondActivityClassName);
                        }, 1, TimeUnit.SECONDS);
                    }
                }
                return true;
            }

            @Override
            public boolean activityResuming(String pkg) {
                return true;
            }

            @Override
            public boolean appCrashed(String processName, int pid,
                                      String shortMsg, String longMsg,
                                      long timeMillis, String stackTrace) {
                return true;
            }

            @Override
            public int appEarlyNotResponding(String processName, int pid, String annotation) {
                return 0;
            }

            @Override
            public int appNotResponding(String processName, int pid, String processStats) {
                return 0;
            }

            @Override
            public int systemNotResponding(String msg) {
                return 0;
            }
        });
    }

    public void stop() {
        ServiceManager.getActivityManager().setActivityController(null);
    }


    public void startSecondActivity() {
        SecondaryDisplayLauncher.startSelfSecondaryLauncher();
        if (Utils.checkActivityReady(mTargetFirstActivity)) {
            startActivity(mTargetSecondActivityPackageName, mTargetSecondActivityClassName);
        }
    }

    @RequiresApi(api = 26)
    private void startActivity(@NonNull String packageName, @NonNull String className) {
        if (ServiceManager.getDisplayManager().isActive(DisplayInfo.getDisplayId())) {
            Intent intent = new Intent();
            intent.setClassName(packageName, className);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(DisplayInfo.getDisplayId());

            int result = ServiceManager.getActivityManager().startActivity(intent, options.toBundle());
            Ln.i(TAG, "startActivity result:" + result);
            if (result < 0) {
                Ln.e(TAG, "Could not start second activity by ActivityManager");
                String activityName = packageName + "/" + className;
                Utils.startActivity(activityName, DisplayInfo.getDisplayId());
            }
        }
    }
}