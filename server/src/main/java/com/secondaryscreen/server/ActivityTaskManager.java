package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.IActivityController;
import android.app.TaskInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ActivityTaskManager {
    public static final String TAG = "ActivityTaskManager";
    private final IInterface mManager;

    static ActivityTaskManager create() {
        IInterface manager = ServiceManager.getService("activity_task", "android.app.IActivityTaskManager");
        return new ActivityTaskManager(manager);
    }

    private ActivityTaskManager(IInterface manager) {
        this.mManager = manager;
    }

    public void getFocusedRootTaskInfo() {
        try {
            Method method = mManager.getClass().getMethod("getAllStackInfosOnDisplay", int.class);
            List<Object> list = (List<Object>)method.invoke(mManager, DisplayInfo.getMirrorDisplayId());

            Class<?> cls = Class.forName("android.app.ActivityManager$StackInfo");
            for (Object info : list) {
                ActivityManager.StackInfo stackInfo = new ActivityManager.StackInfo();
                stackInfo.displayId = cls.getDeclaredField("displayId").getInt(info);
                String[] taskNames = (String[])cls.getDeclaredField("taskNames").get(info);
                stackInfo.taskNames = Arrays.asList(taskNames);
                Ln.i(TAG, "stackInfo.displayId:" + stackInfo.displayId);
                Ln.i(TAG, "stackInfo.taskNames:" + stackInfo.taskNames);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDisplayToSingleTaskInstance(int virtualDisplayId) {
        try {
            Method method = mManager.getClass().getMethod("setDisplayToSingleTaskInstance", int.class);
            method.invoke(mManager, virtualDisplayId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
