package com.secondaryscreen.server;

import android.content.ComponentName;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import android.os.IInterface;
import android.os.UserHandle;

import java.lang.reflect.Method;
import java.util.List;

public final class LauncherApps {
    private static String TAG = "LauncherApps";
    private final IInterface mManager;

    static LauncherApps create() {
        IInterface manager = ServiceManager.getService("launcherapps", "android.content.pm.ILauncherApps");
        return new LauncherApps(manager);
    }

    private LauncherApps(IInterface manager) {
        this.mManager = manager;
    }

    void addOnAppsChangedListener(IOnAppsChangedListener.Stub listener) {
        try {
            Method method = mManager.getClass().getMethod("addOnAppsChangedListener", String.class, IOnAppsChangedListener.class);

            method.invoke(mManager, Utils.PACKAGE_NAME, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void removeOnAppsChangedListener(IOnAppsChangedListener.Stub listener) {
        try {
            Method method = mManager.getClass().getMethod("removeOnAppsChangedListener", IOnAppsChangedListener.class);
            method.invoke(mManager, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<UserHandle> getUserProfiles() {
        try {
            Method method = mManager.getClass().getMethod("getUserProfiles");
            return (List<UserHandle>) method.invoke(mManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void resolveActivity(String callingPackage, ComponentName component, UserHandle user) {
        try {
            Method method = mManager.getClass().getMethod("resolveActivity", String.class, ComponentName.class, UserHandle.class);
            android.content.pm.ActivityInfo activityInfo = (android.content.pm.ActivityInfo) method.invoke(mManager, callingPackage, component, user);
            Ln.i(TAG, "resolveActivity activityInfo:" + activityInfo);
            // Ln.i(TAG, "resolveActivity activityInfo:" + activityInfo.requiredDisplayCategory);
            Ln.i(TAG, "resolveActivity activityInfo:" + activityInfo.applicationInfo.packageName);
            Ln.i(TAG, "resolveActivity activityInfo:" + activityInfo.applicationInfo.packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Bundle getSuspendedPackageLauncherExtras(String packageName, UserHandle user) {
        try {
            Method method = mManager.getClass().getMethod("getSuspendedPackageLauncherExtras", String.class, UserHandle.class);
            Bundle bundle = (Bundle) method.invoke(mManager, packageName, user);
            Ln.i(TAG, "getSuspendedPackageLauncherExtras bundle:" + bundle);
            return bundle;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
