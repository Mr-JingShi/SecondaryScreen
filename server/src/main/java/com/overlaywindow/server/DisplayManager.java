package com.overlaywindow.server;

import android.annotation.SuppressLint;
import android.hardware.display.VirtualDisplay;
import android.view.Surface;

import java.lang.reflect.Method;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/wrappers/DisplayManager.java

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class DisplayManager {
    private final Object mManager; // instance of hidden class android.hardware.display.DisplayManagerGlobal
    private Method mCreateVirtualDisplayMethod;

    static DisplayManager create() {
        try {
            Class<?> clazz = Class.forName("android.hardware.display.DisplayManagerGlobal");
            Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
            Object dmg = getInstanceMethod.invoke(null);
            return new DisplayManager(dmg);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private DisplayManager(Object manager) {
        this.mManager = manager;
    }

    public DisplayInfo getDisplayInfo(int displayId) {
        try {
            Object displayInfo = mManager.getClass().getMethod("getDisplayInfo", int.class).invoke(mManager, displayId);
            Class<?> cls = displayInfo.getClass();

            int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
            int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
            int rotation = cls.getDeclaredField("rotation").getInt(displayInfo);
            int densityDpi = cls.getDeclaredField("logicalDensityDpi").getInt(displayInfo);

            if (width < height) {
                int tmp = width;
                width = height;
                height = tmp;
            }

            System.out.println("width:" + width);
            System.out.println("height:" + height);
            System.out.println("densityDpi:" + densityDpi);

            return new DisplayInfo(new Size(width, height), rotation, densityDpi);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private Method getCreateVirtualDisplayMethod() throws NoSuchMethodException {
        if (mCreateVirtualDisplayMethod == null) {
            mCreateVirtualDisplayMethod = android.hardware.display.DisplayManager.class
                    .getMethod("createVirtualDisplay", String.class, int.class, int.class, int.class, Surface.class);
        }
        return mCreateVirtualDisplayMethod;
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int displayIdToMirror, Surface surface) throws Exception {
        Method method = getCreateVirtualDisplayMethod();
        return (VirtualDisplay) method.invoke(null, name, width, height, displayIdToMirror, surface);
    }
}
