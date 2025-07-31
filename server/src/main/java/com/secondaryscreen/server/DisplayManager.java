package com.secondaryscreen.server;

import android.annotation.SuppressLint;

import java.lang.reflect.Method;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/wrappers/DisplayManager.java

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class DisplayManager {
    private static final String TAG = "DisplayManager";
    private final Object mManager; // instance of hidden class android.hardware.display.DisplayManagerGlobal

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

    public boolean isActive(int displayId) {
        if (displayId != 0) {
            try {
                Object displayInfo = mManager.getClass().getMethod("getDisplayInfo", int.class).invoke(mManager, displayId);
                if (displayInfo != null) {
                    Class<?> cls = displayInfo.getClass();

                    String name = (String) (cls.getDeclaredField("name").get(displayInfo));
                    if (name.equals(Utils.VIRTUALDISPLAY_NAME)) {
                        return true;
                    }
                }
            } catch (ReflectiveOperationException e) {
                Ln.w(TAG, "isActive e:" + e);
            }
        }
        return false;
    }
}
