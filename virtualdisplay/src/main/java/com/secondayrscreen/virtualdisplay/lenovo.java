package com.secondayrscreen.virtualdisplay;

import android.graphics.PixelFormat;
import android.media.ImageReader;
import android.os.IBinder;
import android.os.IInterface;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class lenovo {
    private static final Method GET_SERVICE_METHOD;

    static {
        try {
            GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
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

    static Method findMethodAndMakeAccessible(Method[] methods, String name) throws NoSuchMethodException {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    public static void main(String[] args) {
        try {
            Class<?> BuilderClass = Class.forName("android.hardware.display.VirtualDisplayConfig$Builder");
            Constructor<?> BuilderConstructor = BuilderClass.getConstructor(String.class, int.class, int.class, int.class);
            Object builder = BuilderConstructor.newInstance("virtualdisplay", 1920, 1200, 1/* densityDpi */);

            Method setFlags = BuilderClass.getMethod("setFlags", int.class);
            Method build = BuilderClass.getMethod("build");
            Method setDisplayIdToMirror = BuilderClass.getMethod("setDisplayIdToMirror", int.class);
            Method setSurface = BuilderClass.getMethod("setSurface", Surface.class);

            // VIRTUAL_DISPLAY_FLAG_PUBLIC 1 << 0
            // VIRTUAL_DISPLAY_FLAG_PRESENTATION 1 << 1
            // VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 1 << 4;
            // VIRTUAL_DISPLAY_FLAG_TRUSTED 1 << 10
            // VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY 1 << 3
            // VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT 1 << 7;
            // VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH 1 << 6
            // VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS 1 << 9
            setFlags.invoke(builder, (1<<10)|(1<<0)|(1<<3));// (1<<10)|(1<<0)|(1<<3)
            setDisplayIdToMirror.invoke(builder,-1);

            ImageReader imageReader = ImageReader.newInstance(1920, 1200, PixelFormat.RGBA_8888, 1);
            setSurface.invoke(builder, imageReader.getSurface());

            Object config = build.invoke(builder);

            Class<?> CallbackClass = Class.forName("android.hardware.display.DisplayManagerGlobal$VirtualDisplayCallback");
            Constructor<?>[] CallbackClassConstructor = CallbackClass.getConstructors();
            Object callback = CallbackClassConstructor[0].newInstance(null, null);

            IInterface displayManager = getService("display", "android.hardware.display.IDisplayManager");
            Method createVirtualDisplay = findMethodAndMakeAccessible(displayManager.getClass().getDeclaredMethods(),"createVirtualDisplay");
            String packageName = "com.android.shell";
            int displayId = (int)createVirtualDisplay.invoke(displayManager, config, callback, null, packageName);

            System.out.println("displayId:" + displayId);

            Thread.sleep(10000000);
        } catch (Exception e) {
            System.out.println("lenovo main exception:" + e);
        }
    }
}