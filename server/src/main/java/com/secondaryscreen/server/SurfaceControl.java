package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.ImageReader;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/wrappers/SurfaceControl.java

@SuppressLint("PrivateApi")
public final class SurfaceControl {
    private static final String VIRTUALDISPLAY_NAME_UNSAFE = "PC_virtualdisplay";
    private static final String VIRTUALDISPLAY_NAME = "virtualdisplay";
    private static final Class<?> CLASS;
    private static Object SELF_VIRTUALDISPLAY_TOKEN;

    static {
        try {
            CLASS = Class.forName("android.view.SurfaceControl");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private SurfaceControl() {
        // only static methods
    }

    public static void openTransaction() {
        try {
            CLASS.getMethod("openTransaction").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void closeTransaction() {
        try {
            CLASS.getMethod("closeTransaction").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        try {
            CLASS.getMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class)
                    .invoke(null, displayToken, orientation, layerStackRect, displayRect);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        try {
            CLASS.getMethod("setDisplayLayerStack", IBinder.class, int.class).invoke(null, displayToken, layerStack);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        try {
            CLASS.getMethod("setDisplaySurface", IBinder.class, Surface.class).invoke(null, displayToken, surface);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static IBinder createDisplay(String name, boolean secure) throws Exception {
       return (IBinder) CLASS.getMethod("createDisplay", String.class, boolean.class).invoke(null, name, secure);
    }

    public static int createVirtualDisplay(int width, int height, int densityDpi) throws Exception {
        // 通过反射创建virtualdisplay的灵感来源自Android源码
        // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/core/java/android/hardware/display/DisplayManager.java;drc=b3691fab2356133dfc7e11c213732ffef9a85315;l=1567
        IInterface dm = ServiceManager.getService("display", "android.hardware.display.IDisplayManager");
        Method[] dmMethods = dm.getClass().getDeclaredMethods();
        Method method = findMethodAndMakeAccessible(dmMethods,"createVirtualDisplay");

        Object callback = virtualDisplayCallback();
        // 保存token
        SELF_VIRTUALDISPLAY_TOKEN = callback;

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            Surface surface = getSurface(width, height);

            return (int)method.invoke(
                    /* this */ dm,
                    /* callback */ callback,
                    /* projectionToken */ null,
                    /* packageName */ Utils.PACKAGE_NAME,
                    /* name */ VIRTUALDISPLAY_NAME_UNSAFE,
                    /* width */ width,
                    /* height */ height,
                    /* densityDpi */ densityDpi,
                    /* surface */ surface,
                    /* flags */ getflags(),
                    /* uniqueId */ null);
        }
        Object config = virtualDisplayConfigBuilder(width, height, densityDpi);

        return (int)method.invoke(
                /* this */ dm,
                /* virtualDisplayConfig */ config,
                /* callback */ callback,
                /* projectionToken */ null,
                /* packageName */ Utils.PACKAGE_NAME);
    }

    private static Object virtualDisplayCallback() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Class<?> DisplayManagerGlobalClass = Class.forName("android.hardware.display.DisplayManagerGlobal");
            Class<?>[] innerClasses = DisplayManagerGlobalClass.getDeclaredClasses();
            Class<?> VirtualDisplayCallbackClass = findClassAndMakeAccessible(innerClasses, "android.hardware.display.DisplayManagerGlobal$VirtualDisplayCallback");
            Constructor<?>[] VirtualDisplayCallbackConstructors = VirtualDisplayCallbackClass.getConstructors();

            VirtualDisplayCallbackConstructors[0].setAccessible(true);
            return VirtualDisplayCallbackConstructors[0].newInstance(null, null);
        }

        Class<?> CallbackClass = Class.forName("android.hardware.display.DisplayManagerGlobal$VirtualDisplayCallback");
        Constructor<?>[] CallbackConstructors = CallbackClass.getConstructors();
        return CallbackConstructors[0].newInstance(null, null);
    }

    private static Object virtualDisplayConfigBuilder(int width, int height, int densityDpi) throws Exception {
        Class<?> BuilderClass = Class.forName("android.hardware.display.VirtualDisplayConfig$Builder");
        Constructor<?> BuilderConstructor = BuilderClass.getConstructor(String.class, int.class, int.class, int.class);

        String name = VIRTUALDISPLAY_NAME;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            name = VIRTUALDISPLAY_NAME_UNSAFE;
        }

        Object builder = BuilderConstructor.newInstance(name, width, height, densityDpi);

        Method setFlags = BuilderClass.getMethod("setFlags", int.class);
        setFlags.invoke(builder, getflags());

        Method setSurface = BuilderClass.getMethod("setSurface", Surface.class);
        setSurface.invoke(builder, getSurface(width, height));

        Method build = BuilderClass.getMethod("build");
        return build.invoke(builder);
    }

    private static Surface getSurface(int width, int height) {
        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        return imageReader.getSurface();
    }

    private static int getflags() {
        // VIRTUAL_DISPLAY_FLAG_PUBLIC 1 << 0
        // VIRTUAL_DISPLAY_FLAG_PRESENTATION 1 << 1
        // int VIRTUAL_DISPLAY_FLAG_SECURE 1 << 2
        // VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY 1 << 3
        // VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 1 << 4
        // VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 1 << 5;
        // VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH 1 << 6
        // VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT 1 << 7
        // VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
        // VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS 1 << 9
        // VIRTUAL_DISPLAY_FLAG_TRUSTED 1 << 10
        int flags = (1<<0)|(1<<1)|(1<<3)|(1<<6)|(1<<7)|(1<<9);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flags |= (1<<10);
        }
        return flags;
    }

    private static Class<?> findClassAndMakeAccessible(Class<?>[] innerClasses, String name) throws ClassNotFoundException {
        for (Class<?> clazz : innerClasses) {
            if (clazz.getName().equals(name)) {
                return clazz;
            }
        }
        throw new ClassNotFoundException(name);
    }

    private static Method findMethodAndMakeAccessible(Method[] methods, String name) throws NoSuchMethodException {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    public static void resizeVirtualDisplay(int width, int height, int densityDpi) throws Exception {
        if (SELF_VIRTUALDISPLAY_TOKEN != null) {
            IInterface dm = ServiceManager.getService("display", "android.hardware.display.IDisplayManager");
            Method[] dmMethods = dm.getClass().getDeclaredMethods();
            Method resize = findMethodAndMakeAccessible(dmMethods, "resizeVirtualDisplay");
            resize.invoke(
                    /* this */ dm,
                    /* callback */ SELF_VIRTUALDISPLAY_TOKEN,
                    /* width */ width,
                    /* height */ height,
                    /* densityDpi */ densityDpi);

            Method setSurface = findMethodAndMakeAccessible(dmMethods, "setVirtualDisplaySurface");
            setSurface.invoke(
                    /* this */ dm,
                    /* callback */ SELF_VIRTUALDISPLAY_TOKEN,
                    /* surface */ getSurface(width, height));
        }
    }

    public static void releaseVirtualDisplay() throws Exception {
        if (SELF_VIRTUALDISPLAY_TOKEN != null) {
            IInterface dm = ServiceManager.getService("display", "android.hardware.display.IDisplayManager");
            Method[] dmMethods = dm.getClass().getDeclaredMethods();
            Method method = findMethodAndMakeAccessible(dmMethods, "releaseVirtualDisplay");
            method.invoke(
                    /* this */ dm,
                    /* callback */ SELF_VIRTUALDISPLAY_TOKEN);
        }
    }

    public static void destroyDisplay(IBinder displayToken) {
        try {
            CLASS.getMethod("destroyDisplay", IBinder.class).invoke(null, displayToken);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
