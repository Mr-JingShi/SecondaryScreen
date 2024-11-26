package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.IActivityController;
import android.app.TaskInfo;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;

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
    private static ScheduledExecutorService mExecutor  = Executors.newSingleThreadScheduledExecutor();

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
                        mExecutor.schedule(() -> {
                            if (isReady(mFirstActivity, mSecondActivity)) {
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

        if (isReady(mFirstActivity, mSecondActivity)) {
            startSecondActivity();
        }
    }

    public void stop() {
        ServiceManager.getActivityManager().setActivityController(null);
        mExecutor.shutdown();
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
    private boolean isReady(@NonNull String firstActivity, @NonNull String secondActivity) {
        try {
            List<Pair<Integer, String[]>> list = ServiceManager.getActivityManager().getAllTaskInfos();
            Ln.i(TAG, "list size:" + list.size());

            boolean found = false;
            for (Pair<Integer, String[]> taskInfo : list) {
                for (String activity : taskInfo.second) {
                    // INFO ActivityDetector activity:com.secondaryscreen.sample/com.secondaryscreen.sample.SecondActivity
                    Ln.i(TAG, "activity:" + activity);

                    if (taskInfo.first.intValue() == DisplayInfo.getMirrorDisplayId() && activity.equals(secondActivity)) {
                        return false;
                    } else if (taskInfo.first.intValue() == 0 && activity.equals(firstActivity)) {
                        found = true;
                    }
                }
            }
            return found;
        } catch (ReflectiveOperationException e) {
            Ln.e(TAG, "getAllTaskInfos Could not invoke method", e);

            try {
                List<TaskInfo> list = ServiceManager.getActivityManager().getTasks(9999);
                Ln.i(TAG, "list size:" + list.size());
                boolean found = false;
                for (TaskInfo taskInfo : list) {
                    if (taskInfo.baseIntent.getComponent() != null) {
                        String activity = taskInfo.baseIntent.getComponent().getClassName();
                        // INFO ActivityDetector activity:com.secondaryscreen.sample.SecondActivity
                        Ln.i(TAG, "activity:" + activity);
                        @SuppressLint("BlockedPrivateApi")
                        int displayId = TaskInfo.class.getDeclaredField("displayId").getInt(taskInfo);
                        Ln.i(TAG, "taskInfo.displayId:" + displayId);
                        if (displayId == DisplayInfo.getMirrorDisplayId() && secondActivity.contains(activity)) {
                            return false;
                        } else if (displayId == 0 && firstActivity.contains(activity)) {
                            found = true;
                        }
                    }
                }
                return found;
            } catch (ReflectiveOperationException e1) {
                Ln.e(TAG, "getTasks Could not invoke method", e1);
            }
        }

        return !Utils.activityRunning(secondActivity) && Utils.activityRunning(firstActivity);
    }
}