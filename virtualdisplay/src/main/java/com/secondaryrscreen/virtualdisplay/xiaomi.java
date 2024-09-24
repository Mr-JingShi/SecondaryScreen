package com.secondaryscreen.virtualdisplay;

import android.graphics.PixelFormat;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.IBinder;
import android.os.IInterface;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class xiaomi {
    private static final Method GET_SERVICE_METHOD;
    private static String VIDEO_FORMAT = "video/avc";

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
            setFlags.invoke(builder, (1<<0)|(1<<1));// (1<<0)|(1<<1)
            setDisplayIdToMirror.invoke(builder,-1);

            MediaCodec mediaCodec = MediaCodec.createEncoderByType(VIDEO_FORMAT);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(VIDEO_FORMAT, 1920, 1200);
            mediaCodec.configure(mediaFormat, null, null, 0);

            setSurface.invoke(builder, mediaCodec.createInputSurface());

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

            mediaCodec.start();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mediaCodec.dequeueOutputBuffer(bufferInfo, -1);

            mediaCodec.stop();
            mediaCodec.release();

            Thread.sleep(10000000);
        } catch (Exception e) {
            System.out.println("lenovo main exception:" + e);
        }
    }
}