package com.secondaryscreen.server;

import android.os.IInterface;
import android.view.IRotationWatcher;

import java.lang.reflect.Method;

public final class WindowManager {
    private final IInterface manager;
    private Method getRotationMethod;
    private RotationListener mRotationWatcher;
    public interface RotationListener {
        void onRotationChanged(int rotation, DisplayInfo displayInfo);
    }

    static WindowManager create() {
        IInterface manager = ServiceManager.getService("window", "android.view.IWindowManager");
        return new WindowManager(manager);
    }

    private WindowManager(IInterface manager) {
        this.manager = manager;
    }

    private Method getGetRotationMethod() throws NoSuchMethodException {
        if (getRotationMethod == null) {
            Class<?> cls = manager.getClass();
            try {
                // method changed since this commit:
                // https://android.googlesource.com/platform/frameworks/base/+/8ee7285128c3843401d4c4d0412cd66e86ba49e3%5E%21/#F2
                getRotationMethod = cls.getMethod("getDefaultDisplayRotation");
            } catch (NoSuchMethodException e) {
                // old version
                getRotationMethod = cls.getMethod("getRotation");
            }
        }
        return getRotationMethod;
    }

    public int getRotation() {
        try {
            Method method = getGetRotationMethod();
            return (int) method.invoke(manager);
        } catch (ReflectiveOperationException e) {
            System.out.println("Could not invoke method" + e);
            return 0;
        }
    }

    public void registerRotationWatcher(IRotationWatcher rotationWatcher, int displayId) {
        try {
            Class<?> cls = manager.getClass();
            try {
                // display parameter added since this commit:
                // https://android.googlesource.com/platform/frameworks/base/+/35fa3c26adcb5f6577849fd0df5228b1f67cf2c6%5E%21/#F1
                cls.getMethod("watchRotation", IRotationWatcher.class, int.class).invoke(manager, rotationWatcher, displayId);
            } catch (NoSuchMethodException e) {
                // old version
                cls.getMethod("watchRotation", IRotationWatcher.class).invoke(manager, rotationWatcher);
            }
        } catch (Exception e) {
            System.out.println("Could not register rotation watcher" + e);
        }
    }

    public void setRotationListener(RotationListener rotationListener) {
        mRotationWatcher = rotationListener;
    }

    public void onRotationChanged(int rotation, DisplayInfo displayInfo) {
        if (mRotationWatcher != null) {
            mRotationWatcher.onRotationChanged(rotation, displayInfo);
        }
    }
}
