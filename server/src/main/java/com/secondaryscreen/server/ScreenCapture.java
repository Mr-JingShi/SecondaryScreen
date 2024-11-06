package com.secondaryscreen.server;

import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/ScreenCapture.java

public class ScreenCapture implements WindowManager.RotationListener, DisplayManager.DisplayListener {
    private static final String TAG = "ScreenCapture";
    private final AtomicBoolean mResetCapture = new AtomicBoolean();
    private final AtomicBoolean mRestartCapture = new AtomicBoolean();
    private static String VIRTUALDISPLAY = "virtualdisplay";
    private ScreenInfo mScreenInfo;
    private IBinder mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private String mRemoteAddress;
    private Lock mLock = new ReentrantLock();
    private Condition mCondition = mLock.newCondition();

    public ScreenCapture() {
        ServiceManager.getWindowManager().setRotationListener(this);
        ServiceManager.getDisplayManager().setDisplayListener(this);
    }

    public void computeScreenInfo() {
        DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(false);
        int maxSize = SurfaceEncoder.chooseMaxSize(displayInfo.getSize());

        mScreenInfo = ScreenInfo.computeScreenInfo(displayInfo.getRotation(), displayInfo.getSize(), null, maxSize, -1);
    }

    @Override
    public void onRotationChanged(int rotation) {
        if (mScreenInfo != null) {
            mScreenInfo = mScreenInfo.withDeviceRotation(rotation);
        }

        requestReset();
    }

    @Override
    public void onDisplayChanged(String remoteAddress) {
        setRemoteAddress(remoteAddress);

        requestRestart();
    }

    protected void requestReset() {
        mResetCapture.set(true);
    }

    public boolean consumeReset() {
        return mResetCapture.getAndSet(false);
    }

    protected void requestRestart() {
        mRestartCapture.set(true);
    }

    public boolean restartReset() {
        return mRestartCapture.getAndSet(false);
    }

    private void setRemoteAddress(String remoteAddress) {
        mLock.lock();
        try {
            mRemoteAddress = remoteAddress;
            mCondition.signal();
        } finally {
            mLock.unlock();
        }
    }

    public String getRemoteAddress() throws InterruptedException {
        String remoteAddress = null;
        mLock.lock();
        try {
            while (mRemoteAddress == null) {
                mCondition.await();
            }
            remoteAddress = mRemoteAddress;
            mRemoteAddress = null;
        } finally {
            mLock.unlock();
        }
        return remoteAddress;
    }

    public void start(Surface surface) {
        if (mDisplay != null) {
            SurfaceControl.destroyDisplay(mDisplay);
            mDisplay = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        int mirrorDisplayId = DisplayInfo.getMirrorDisplayId();
        try {
            Rect contentRect = mScreenInfo.getContentRect();
            Ln.d(TAG, "contentRect:" + contentRect);
            // does not include the locked video orientation
            Rect unlockedVideoRect = mScreenInfo.getUnlockedVideoSize().toRect();
            Ln.d(TAG, "unlockedVideoRect:" + unlockedVideoRect);
            int videoRotation = mScreenInfo.getVideoRotation();
            Ln.d(TAG, "videoRotation:" + videoRotation);

            mDisplay = createDisplay();
            setDisplaySurface(mDisplay, surface, videoRotation, contentRect, unlockedVideoRect, mirrorDisplayId);
            Ln.i(TAG, "Display: using SurfaceControl API");
        } catch (Exception surfaceControlException) {
            Ln.w(TAG, "surfaceControlException", surfaceControlException);
            try {
                Rect videoRect = mScreenInfo.getVideoSize().toRect();
                Ln.d(TAG, "videoRotation:" + videoRect);
                mVirtualDisplay = ServiceManager.getDisplayManager().createVirtualDisplay(VIRTUALDISPLAY, videoRect.width(), videoRect.height(), mirrorDisplayId, surface);
                Ln.i(TAG, "Display: using DisplayManager API");
            } catch (Exception displayManagerException) {
                Ln.w(TAG, "Could not create display using DisplayManager", displayManagerException);
                throw new AssertionError("Could not create display");
            }
        }
    }

    public void release() {
        ServiceManager.getDisplayManager().setDisplayListener(null);
        ServiceManager.getWindowManager().setRotationListener(null);
        if (mDisplay != null) {
            SurfaceControl.destroyDisplay(mDisplay);
            mDisplay = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    public Size getSize() {
        return mScreenInfo.getVideoSize();
    }

    public boolean setMaxSize(int maxSize) {
        DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(false);
        mScreenInfo = ScreenInfo.computeScreenInfo(mScreenInfo.getDeviceRotation(), displayInfo.getSize(), null, maxSize, -1);
        return true;
    }

    private static IBinder createDisplay() throws Exception {
        // Since Android 12 (preview), secure displays could not be created with shell permissions anymore.
        // On Android 12 preview, SDK_INT is still R (not S), but CODENAME is "S".
        boolean secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(
                Build.VERSION.CODENAME));
        return SurfaceControl.createDisplay(VIRTUALDISPLAY, secure);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, int orientation, Rect deviceRect, Rect displayRect, int layerStack) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, orientation, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, layerStack);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }
}
