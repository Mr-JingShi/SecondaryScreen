package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.IActivityController;
import android.app.TaskInfo;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityDetector {
    private static String TAG = "ActivityDetector";
    @NonNull
    private String mFirstActivity;
    @NonNull
    private String mSecondActivity;
    @NonNull
    private String mSecondActivityPackage;
    @NonNull
    private String mSecondActivityClassName;

    public ActivityDetector(String firstActivity, String secondActivity) {
        this.mFirstActivity = firstActivity;
        this.mSecondActivity = secondActivity;
        this.mSecondActivityPackage = mSecondActivity.substring(0, mSecondActivity.indexOf("/"));
        this.mSecondActivityClassName = mSecondActivity.substring(mSecondActivity.indexOf("/") + 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void start() {
        ServiceManager.getActivityManager().setActivityController(new IActivityController.Stub() {
            @Override
            public boolean activityStarting(Intent intent, String pkg) {
                Ln.i(TAG, "intent:" + intent);
                if (intent.getComponent() != null) {
                    String activityName = intent.getComponent().getClassName();
                    Ln.i(TAG, "activityStarting:" + activityName + " FirstActivity:" + mFirstActivity);
                    if(mFirstActivity.contains(activityName)) {
                        Utils.schedule(() -> {
                            if (Utils.isActivityReady(mFirstActivity, mSecondActivity)) {
                                Ln.i(TAG, "First activity started, but second activity have not start, start second activity");
                                startSecondActivity();
                            }
                        }, 2, TimeUnit.SECONDS);
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

        if (Utils.isActivityReady(mFirstActivity, mSecondActivity)) {
            startSecondActivity();
        }
    }

    public void stop() {
        ServiceManager.getActivityManager().setActivityController(null);
    }

    @RequiresApi(api = 26)
    private void startSecondActivity() {
        Intent intent = new Intent();
        intent.setClassName(mSecondActivityPackage, mSecondActivityClassName);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());

        int result = ServiceManager.getActivityManager().startActivity(intent, options.toBundle());
        Ln.i(TAG, "startSecondActivity result:" + result);
        if (result < 0) {
            Ln.e(TAG, "Could not start second activity by ActivityManager");
            Utils.startActivity(mSecondActivity, DisplayInfo.getMirrorDisplayId());
        }
    }
}