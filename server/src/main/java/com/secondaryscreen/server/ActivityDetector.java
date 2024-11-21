package com.secondaryscreen.server;

import android.app.ActivityOptions;
import android.app.IActivityController;
import android.app.TaskInfo;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

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
                if (intent.getComponent() != null) {
                    String activityName = intent.getComponent().getClassName();
                    Ln.i(TAG, "activityStarting:" + activityName);
                    if(mFirstActivity.contains(activityName)) {
                        if (isReady(null, mSecondActivity)) {
                            Ln.i(TAG, "First activity started, but second activity have not start, start second activity");
                            startSecondActivity();
                        }
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

        if (isReady(mFirstActivity, mSecondActivity)) {
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

    @RequiresApi(api = 29)
    private boolean isReady(@Nullable String firstActivity, @NonNull String secondActivity) {
        List<TaskInfo> list = ServiceManager.getActivityManager().getAllRootTaskInfos();
        if (list != null) {
            boolean found = firstActivity == null ? true : false;

            for (TaskInfo info : list) {
                if (info.baseIntent.getComponent() != null) {
                    String name = info.baseIntent.getComponent().getClassName();
                    Ln.i(TAG, "task:" + info.baseIntent.getComponent().getClassName());
                    if (secondActivity.contains(name)) {
                        return false;
                    } else if (firstActivity != null && firstActivity.contains(name)) {
                        found = true;
                    }
                }
            }
            return found;
        }
        return !Utils.activityRunning(secondActivity) && (firstActivity == null ? true : Utils.activityRunning(firstActivity));
    }
}