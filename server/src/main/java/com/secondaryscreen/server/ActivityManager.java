package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.IActivityController;
import android.app.TaskInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;
import java.util.List;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ActivityManager {
    public static final String TAG = "ActivityManager";
    private final IInterface mManager;
    static ActivityManager create() {
        try {
            // On old Android versions, the ActivityManager is not exposed via AIDL,
            // so use ActivityManagerNative.getDefault()
            Class<?> cls = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = cls.getDeclaredMethod("getDefault");
            IInterface am = (IInterface) getDefaultMethod.invoke(null);
            return new ActivityManager(am);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private ActivityManager(IInterface manager) {
        this.mManager = manager;
    }

    @SuppressWarnings("ConstantConditions")
    public int startActivity(Intent intent, Bundle bOptions) {
        try {
            Class<?> iApplicationThreadClass = Class.forName("android.app.IApplicationThread");
            Class<?> profilerInfo = Class.forName("android.app.ProfilerInfo");
            Method method = mManager.getClass()
                    .getMethod("startActivityAsUser", iApplicationThreadClass, String.class, Intent.class, String.class, IBinder.class, String.class,
                            int.class, int.class, profilerInfo, Bundle.class, int.class);

            return (int) method.invoke(
                    /* this */ mManager,
                    /* caller */ null,
                    /* callingPackage */ Utils.PACKAGE_NAME,
                    /* intent */ intent,
                    /* resolvedType */ null,
                    /* resultTo */ null,
                    /* resultWho */ null,
                    /* requestCode */ 0,
                    /* startFlags */ 0,
                    /* profilerInfo */ null,
                    /* bOptions */ bOptions,
                    /* userId */ /* UserHandle.USER_CURRENT */ -2);
        } catch (Throwable e) {
            Ln.e(TAG, "Could not invoke method", e);
            return -1;
        }
    }

    public List<TaskInfo> getAllRootTaskInfos() {
        try {
            Method method = mManager.getClass().getMethod("getAllRootTaskInfos");
            return (List<TaskInfo>)method.invoke(mManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setActivityController(IActivityController.Stub stub) {
        try {
            Method method = mManager.getClass().getMethod("setActivityController", IActivityController.class, boolean.class);
            method.invoke(mManager, stub, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
