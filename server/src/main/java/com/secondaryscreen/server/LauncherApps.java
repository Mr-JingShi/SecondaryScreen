package com.secondaryscreen.server;

import android.content.pm.IOnAppsChangedListener;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import android.os.IInterface;
import android.os.UserHandle;

import java.lang.reflect.Method;

public final class LauncherApps {
    private static String TAG = "PackageManager";
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
}
