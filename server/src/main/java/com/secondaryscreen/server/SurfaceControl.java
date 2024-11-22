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
                    /* name */ VIRTUALDISPLAY_NAME,
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

        Object builder = BuilderConstructor.newInstance(VIRTUALDISPLAY_NAME, width, height, densityDpi);

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
        /** remark
         * Android 10 FLAG_PRESENTATION
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1, displayId 14", uniqueId "overlay:1", app 1920 x 1080, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, mode 22, defaultMode 22, modes [{id=22, width=1920, height=1080, fps=60.000004}], colorMode 0, supportedColorModes [0], hdrCapabilities null, rotation 0, density 320 (320.0 x 320.0) dpi, layerStack 14, appVsyncOff 0, presDeadline 33333332, type OVERLAY, state ON, FLAG_PRESENTATION, removeMode 0}
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1, displayId 13", uniqueId "overlay:1", app 1920 x 1080, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, mode 21, defaultMode 21, modes [{id=21, width=1920, height=1080, fps=60.000004}], colorMode 0, supportedColorModes [0], hdrCapabilities null, rotation 0, density 320 (320.0 x 320.0) dpi, layerStack 13, appVsyncOff 0, presDeadline 33333332, type OVERLAY, state ON, FLAG_SECURE, FLAG_PRESENTATION, removeMode 0}
         *
         * Android 11 FLAG_PRESENTATION, FLAG_TRUSTED
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 22, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 33333332, mode 23, defaultMode 23, modes [{id=23, width=1920, height=1080, fps=60.000004}], hdrCapabilities null, minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 22, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0}
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 24, FLAG_SECURE, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 33333332, mode 25, defaultMode 25, modes [{id=25, width=1920, height=1080, fps=60.000004}], hdrCapabilities null, minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 24, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0}
         * 自定义virtualdisplay添加但是添加VIRTUAL_DISPLAY_FLAG_TRUSTED时报错，添加ADD_TRUSTED_DISPLAY也无效
         * java.lang.reflect.InvocationTargetException
         * at java.lang.reflect.Method.invoke(Native Method)
         * at com.secondaryscreen.server.SurfaceControl.createVirtualDisplay(SurfaceControl.java:110)
         * at com.secondaryscreen.server.Server.main(Server.java:38)
         * at com.android.internal.os.RuntimeInit.nativeFinishInit(Native Method)
         * at com.android.internal.os.RuntimeInit.main(RuntimeInit.java:463)
         * Caused by: java.lang.SecurityException: Requires ADD_TRUSTED_DISPLAY permission to create a trusted virtual display.
         * at android.os.Parcel.createExceptionOrNull(Parcel.java:2376)
         * at android.os.Parcel.createException(Parcel.java:2360)
         * at android.os.Parcel.readException(Parcel.java:2343)
         * at android.os.Parcel.readException(Parcel.java:2285)
         * at android.hardware.display.IDisplayManager$Stub$Proxy.createVirtualDisplay(IDisplayManager.java:1085)
         * ... 5 more
         * Caused by: android.os.RemoteException: Remote stack trace:
         * at com.android.server.display.DisplayManagerService$BinderService.createVirtualDisplay(DisplayManagerService.java:2273)
         * at android.hardware.display.IDisplayManager$Stub.onTransact(IDisplayManager.java:519)
         * at com.android.server.display.DisplayManagerService$BinderService.onTransact(DisplayManagerService.java:2638)
         * at android.os.Binder.execTransactInternal(Binder.java:1157)
         * at android.os.Binder.execTransact(Binder.java:1126)
         *
         * Android 12 FLAG_PRESENTATION, FLAG_TRUSTED
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 935", displayGroupId 0, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 33333332, mode 942, defaultMode 942, modes [{id=942, width=1920, height=1080, fps=60.000004, alternativeRefreshRates=[]}], hdrCapabilities null, userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 935, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0, refreshRateOverride 0.0, brightnessMinimum 0.0, brightnessMaximum 0.0, brightnessDefault 0.0}
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 936", displayGroupId 0, FLAG_SECURE, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 33333332, mode 943, defaultMode 943, modes [{id=943, width=1920, height=1080, fps=60.000004, alternativeRefreshRates=[]}], hdrCapabilities null, userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 936, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0, refreshRateOverride 0.0, brightnessMinimum 0.0, brightnessMaximum 0.0, brightnessDefault 0.0}
         * 同Android 11，自定义virtualdisplay添加但是添加VIRTUAL_DISPLAY_FLAG_TRUSTED时报错，添加ADD_TRUSTED_DISPLAY也无效
         *
         * Android 13
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 7", displayGroupId 0, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 28222221, mode 11, defaultMode 11, modes [{id=11, width=1920, height=1080, fps=90.0, alternativeRefreshRates=[]}], hdrCapabilities null, userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 7, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0, refreshRateOverride 0.0, brightnessMinimum 0.0, brightnessMaximum 0.0, brightnessDefault 0.0, installOrientation ROTATION_0}
         * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 6", displayGroupId 0, FLAG_SECURE, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 28222221, mode 10, defaultMode 10, modes [{id=10, width=1920, height=1080, fps=90.0, alternativeRefreshRates=[]}], hdrCapabilities null, userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 6, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0, refreshRateOverride 0.0, brightnessMinimum 0.0, brightnessMaximum 0.0, brightnessDefault 0.0, installOrientation ROTATION_0}
         * 自定义virtualdisplay时允许添加VIRTUAL_DISPLAY_FLAG_TRUSTED
         */

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
        int flags = (1<<1)|(1<<3)|(1<<6)|(1<<7)|(1<<9);
        /** remark
         * Android 10 ～ 12 pirvate的virtualdisplay对ADB SHELL和APP均可见，ADB SHELL有权限使用，但APP无权限使用
         * Android 13+ pirvate的virtualdisplay对ADB SHELL和APP均不可见
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flags = flags | (1<<0) | (1<<10);
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
