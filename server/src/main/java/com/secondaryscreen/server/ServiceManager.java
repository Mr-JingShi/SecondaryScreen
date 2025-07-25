package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/wrappers/ServiceManager.java

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ServiceManager {

    private static final Method GET_SERVICE_METHOD;

    static {
        try {
            GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static DisplayManager mDisplayManager;
    private static InputManager mInputManager;
    private static ActivityManager mActivityManager;
    private static PackageManager mPackageManager;
    private static LauncherApps mLauncherApps;

    private ServiceManager() {
        /* not instantiable */
    }

    static IInterface getService(String service, String type) {
        try {
            IBinder binder = (IBinder) GET_SERVICE_METHOD.invoke(null, service);
            Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static DisplayManager getDisplayManager() {
        if (mDisplayManager == null) {
            mDisplayManager = DisplayManager.create();
        }
        return mDisplayManager;
    }

    public static InputManager getInputManager() {
        if (mInputManager == null) {
            mInputManager = InputManager.create();
        }
        return mInputManager;
    }

    public static ActivityManager getActivityManager() {
        if (mActivityManager == null) {
            mActivityManager = ActivityManager.create();
        }
        return mActivityManager;
    }

    public static PackageManager getPackageManager() {
        if (mPackageManager == null) {
            mPackageManager = PackageManager.create();
        }
        return mPackageManager;
    }

    public static LauncherApps getLauncherApps() {
        if (mLauncherApps == null) {
            mLauncherApps = LauncherApps.create();
        }
        return mLauncherApps;
    }
}
