package com.overlaywindow.server;

import android.annotation.SuppressLint;
import android.view.InputEvent;

import java.lang.reflect.Method;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class InputManager {

    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;

    private final Object mManager;
    private Method mInjectInputEventMethod;

    private static Method mSetDisplayIdMethod;

    static InputManager create() {
        try {
            Class<?> inputManagerClass = getInputManagerClass();
            Method getInstanceMethod = inputManagerClass.getDeclaredMethod("getInstance");
            Object im = getInstanceMethod.invoke(null);
            return new InputManager(im);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Class<?> getInputManagerClass() {
        try {
            // Parts of the InputManager class have been moved to a new InputManagerGlobal class in Android 14 preview
            return Class.forName("android.hardware.input.InputManagerGlobal");
        } catch (ClassNotFoundException e) {
            return android.hardware.input.InputManager.class;
        }
    }

    private InputManager(Object manager) {
        this.mManager = manager;
    }

    private Method getInjectInputEventMethod() throws NoSuchMethodException {
        if (mInjectInputEventMethod == null) {
            mInjectInputEventMethod = mManager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        }
        return mInjectInputEventMethod;
    }

    public boolean injectInputEvent(InputEvent inputEvent) {
        try {
            Method method = getInjectInputEventMethod();
            return (boolean) method.invoke(mManager, inputEvent, INJECT_INPUT_EVENT_MODE_ASYNC);
        } catch (ReflectiveOperationException e) {
            System.out.println("Could not invoke method:" + e);
            return false;
        }
    }

    private static Method getSetDisplayIdMethod() throws NoSuchMethodException {
        if (mSetDisplayIdMethod == null) {
            mSetDisplayIdMethod = InputEvent.class.getMethod("setDisplayId", int.class);
        }
        return mSetDisplayIdMethod;
    }

    public static boolean setDisplayId(InputEvent inputEvent, int displayId) {
        try {
            Method method = getSetDisplayIdMethod();
            method.invoke(inputEvent, displayId);
            return true;
        } catch (ReflectiveOperationException e) {
            System.out.println("Cannot associate a display id to the input event:" + e);
            return false;
        }
    }
}
