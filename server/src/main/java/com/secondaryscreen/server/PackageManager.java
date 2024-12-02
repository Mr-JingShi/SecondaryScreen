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
    private Method mResolveIntentMethod;
    private int mResolveIntentMethodVersion;

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

    private Method getResolveIntentMethod() throws NoSuchMethodException {
        if (mResolveIntentMethod == null) {
            try {
                mResolveIntentMethod = mManager.getClass().getMethod("resolveIntent", Intent.class, String.class, long.class, int.class);
                mResolveIntentMethodVersion = 0;
            } catch (Exception e) {
                mResolveIntentMethod = mManager.getClass().getMethod("resolveIntent", Intent.class, String.class, int.class, int.class);
                mResolveIntentMethodVersion = 1;
            }
        }
        return mResolveIntentMethod;
    }

    ResolveInfo resolveActivity(Intent intent, long flags, int userId) {
        try {
            Method method = getResolveIntentMethod();

            switch (mResolveIntentMethodVersion) {
                case 0:
                    return (ResolveInfo) method.invoke(mManager,
                            /* intent */ intent,
                            /* resolvedType */ null,
                            /* flags */ flags,
                            /* userId */ userId);
                default:
                    return (ResolveInfo) method.invoke(mManager,
                            /* intent */ intent,
                            /* resolvedType */ null,
                            /* flags */ (int)flags,
                            /* userId */ userId);
            }
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
        }
        return null;
    }
}
