package com.secondaryscreen.server;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.IInterface;
import android.os.IRemoteCallback;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

public final class PackageManager {
    private static String TAG = "PackageManager";
    private final IInterface mManager;

    static PackageManager create() {
        IInterface manager = ServiceManager.getService("package", "android.content.pm.IPackageManager");
        return new PackageManager(manager);
    }

    private PackageManager(IInterface manager) {
        this.mManager = manager;
    }

    @RequiresApi(api = 34)
    void registerPackageMonitorCallback(IRemoteCallback callback, int userId) {
        try {
            Method method = mManager.getClass().getMethod("registerPackageMonitorCallback", IRemoteCallback.class, int.class);
            method.invoke(mManager, callback, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = 34)
    void unregisterPackageMonitorCallback(IRemoteCallback callback) {
        try {
            Method method = mManager.getClass().getMethod("unregisterPackageMonitorCallback", IRemoteCallback.class);
            method.invoke(mManager, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
