package com.overlaywindow.server;

import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/ScreenCapture.java

public class ScreenCapture {
    private static String VIRTUALDISPLAY = "virtualdisplay";
    private ScreenInfo mScreenInfo;
    private DisplayInfo mDisplayInfo;
    private IBinder mDisplay;
    private VirtualDisplay mVirtualDisplay;

    public ScreenCapture(DisplayInfo displayInfo) {
        this.mDisplayInfo = displayInfo;
        this.mScreenInfo = ScreenInfo.computeScreenInfo(displayInfo.getRotation(), displayInfo.getSize(), null, 0, -1);
    }

    public void start(Surface surface) {
        Rect contentRect = mScreenInfo.getContentRect();

        // does not include the locked video orientation
        Rect unlockedVideoRect = mScreenInfo.getUnlockedVideoSize().toRect();
        int videoRotation = mScreenInfo.getVideoRotation();
        int mirrorDisplayId = mDisplayInfo.getMirrorDisplayId();

        if (mDisplay != null) {
            SurfaceControl.destroyDisplay(mDisplay);
            mDisplay = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        try {
            mDisplay = createDisplay();
            setDisplaySurface(mDisplay, surface, videoRotation, contentRect, unlockedVideoRect, mirrorDisplayId);
            System.out.println("Display: using SurfaceControl API");
        } catch (Exception surfaceControlException) {
            System.out.println("surfaceControlException:" + surfaceControlException);
            Rect videoRect = mScreenInfo.getVideoSize().toRect();
            try {
                mVirtualDisplay = ServiceManager.getDisplayManager().createVirtualDisplay(VIRTUALDISPLAY, videoRect.width(), videoRect.height(), mirrorDisplayId, surface);
                System.out.println("Display: using DisplayManager API");
            } catch (Exception displayManagerException) {
                System.out.println("Could not create display using DisplayManager" + displayManagerException);
                throw new AssertionError("Could not create display");
            }
        }
    }

    public void release() {
        if (mDisplay != null) {
            SurfaceControl.destroyDisplay(mDisplay);
        }
    }

    public Size getSize() {
        return mScreenInfo.getVideoSize();
    }

    public boolean setMaxSize(int maxSize) {
        mScreenInfo = ScreenInfo.computeScreenInfo(mScreenInfo.getReverseVideoRotation(), mDisplayInfo.getSize(), null, maxSize, -1);
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
