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
        // 1. 优先启动SecondaryDisplayLauncher
        // Android 11 ~ 12 需启动SecondaryDisplayLauncher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                || Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            SecondaryDisplayLauncher.start();
        }

        // 2. 监听Activity启动
        ServiceManager.getActivityManager().setActivityController(new IActivityController.Stub() {
            private boolean mAppSecondActivityStartBySelf = false;
            @Override
            public boolean activityStarting(Intent intent, String pkg) {
                if (intent.getComponent() != null) {
                    String packageName = intent.getComponent().getPackageName();
                    String className = intent.getComponent().getClassName();
                    String activityName = packageName + "/" + className;
                    Ln.i(TAG, "activityStarting activityName:" + activityName);

                    final int targetFlag = 1 << 0;
                    final int appFlag = 1 << 1;
                    int startFlag = 0;
                    if (mTargetFirstActivity.equals(activityName)) {
                        startFlag |= targetFlag;
                    } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            || Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            && Utils.APP_MAIN_ACTIVITY_NAME.equals(activityName)) {
                        /* remark
                        * Android 10 ～ 12 的SecondaryDisplayLauncher实际场景基本用不到，各个厂商的界面效果相差很大，
                        * 小米的一款Android 10设备SecondaryDisplayLauncher直接把背景色设置为全黑，很丑，这里尝试做一个自己副屏桌面
                        */
                        startFlag |= appFlag;
                    }

                    if (startFlag != 0) {
                        final int finalStartFlag = startFlag;
                        Utils.schedule(() -> {
                            ArrayList<Pair<String, String>> lists = new ArrayList<>();
                            lists.add(new Pair<>(mTargetFirstActivity, mTargetSecondActivity));
                            lists.add(new Pair<>(Utils.APP_MAIN_ACTIVITY_NAME, Utils.APP_SECOND_ACTIVITY_NAME));

                            List<Pair<Boolean, Boolean>> result = Utils.checkActivityReady(lists);

                            Ln.i(TAG, "activityStarting startFlag:" + finalStartFlag);

                            Pair<Boolean, Boolean> targetResult = result.get(0);
                            Pair<Boolean, Boolean> appResult = result.get(1);
                            Ln.i(TAG, "activityStarting targetResult.first:" + targetResult.first + ", targetResult.second:" + targetResult.second);
                            Ln.i(TAG, "activityStarting appResult.first:" + appResult.first + ", appResult.second:" + appResult.second);

                            if ((finalStartFlag & targetFlag) != 0) {
                                if (targetResult.first && !targetResult.second) {
                                    if (/*appResult.first && */!appResult.second) {
                                        startSecondActivity(Utils.APP_PACKAGE_NAME, Utils.APP_SECOND_ACTIVITY_CLASS_NAME);
                                    }
                                    startSecondActivity(mTargetSecondActivityPackageName, mTargetSecondActivityClassName);
                                }
                            } else if ((finalStartFlag & appFlag) != 0) {
                                if (/*appResult.first && */!appResult.second) {
                                    startSecondActivity(Utils.APP_PACKAGE_NAME, Utils.APP_SECOND_ACTIVITY_CLASS_NAME);

                                    if (/*targetResult.first && */targetResult.second) {
                                        startSecondActivity(mTargetSecondActivityPackageName, mTargetSecondActivityClassName);
                                    }
                                }
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

        // 3. 检查是否需要启动SecondActivity
        ArrayList<Pair<String, String>> lists = new ArrayList<>();
        lists.add(new Pair<>(mTargetFirstActivity, mTargetSecondActivity));
        List<Pair<Boolean, Boolean>> result = Utils.checkActivityReady(lists);
        Ln.i(TAG, "ActivityDetector result.get(0).first:" + result.get(0).first + ", result.get(0).second:" + result.get(0).second);
        if (result.get(0).first && !result.get(0).second) {
            startSecondActivity(mTargetSecondActivityPackageName, mTargetSecondActivityClassName);
        }
    }

    public void stop() {
        ServiceManager.getActivityManager().setActivityController(null);
    }

    @RequiresApi(api = 26)
    private void startSecondActivity(@NonNull String packageName, @NonNull String className) {
        Intent intent = new Intent();
        intent.setClassName(packageName, className);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());

        int result = ServiceManager.getActivityManager().startActivity(intent, options.toBundle());
        Ln.i(TAG, "startSecondActivity result:" + result);
        if (result < 0) {
            Ln.e(TAG, "Could not start second activity by ActivityManager");
            Utils.startActivity(mTargetSecondActivity, DisplayInfo.getMirrorDisplayId());
        }
    }
}