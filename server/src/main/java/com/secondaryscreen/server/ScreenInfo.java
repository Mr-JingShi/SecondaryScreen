package com.secondaryscreen.server;

import android.graphics.Rect;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/ScreenInfo.java

public final class ScreenInfo {
    private static final String TAG = "ScreenInfo";
    private static final int LOCK_VIDEO_ORIENTATION_INITIAL = -2;
    /**
     * Device (physical) size, possibly cropped
     */
    private final Rect mContentRect; // device size, possibly cropped

    /**
     * Video size, possibly smaller than the device size, already taking the device rotation and crop into account.
     * <p>
     * However, it does not include the locked video orientation.
     */
    private final Size mUnlockedVideoSize;

    /**
     * Device rotation, related to the natural device orientation (0, 1, 2 or 3)
     */
    private final int mDeviceRotation;

    /**
     * The locked video orientation (-1: disabled, 0: normal, 1: 90° CCW, 2: 180°, 3: 90° CW)
     */
    private final int mLockedVideoOrientation;

    public ScreenInfo(Rect contentRect, Size unlockedVideoSize, int deviceRotation, int lockedVideoOrientation) {
        Ln.d(TAG, "deviceRotation:" + deviceRotation);
        this.mContentRect = contentRect;
        this.mUnlockedVideoSize = unlockedVideoSize;
        this.mDeviceRotation = deviceRotation;
        this.mLockedVideoOrientation = lockedVideoOrientation;
    }

    public Rect getContentRect() {
        return mContentRect;
    }

    /**
     * Return the video size as if locked video orientation was not set.
     *
     * @return the unlocked video size
     */
    public Size getUnlockedVideoSize() {
        return mUnlockedVideoSize;
    }

    /**
     * Return the actual video size if locked video orientation is set.
     *
     * @return the actual video size
     */
    public Size getVideoSize() {
        if (getVideoRotation() % 2 == 0) {
            return mUnlockedVideoSize;
        }

        return mUnlockedVideoSize.rotate();
    }

    public int getDeviceRotation() {
        return mDeviceRotation;
    }

    public ScreenInfo withDeviceRotation(int newDeviceRotation) {
        Ln.d(TAG, "newDeviceRotation:" + newDeviceRotation + " oldDeviceRotation:" + mDeviceRotation);
        if (newDeviceRotation == mDeviceRotation) {
            return this;
        }
        // true if changed between portrait and landscape
        boolean orientationChanged = (mDeviceRotation + newDeviceRotation) % 2 != 0;
        Rect newContentRect;
        Size newUnlockedVideoSize;
        if (orientationChanged) {
            newContentRect = flipRect(mContentRect);
            newUnlockedVideoSize = mUnlockedVideoSize.rotate();
        } else {
            newContentRect = mContentRect;
            newUnlockedVideoSize = mUnlockedVideoSize;
        }
        return new ScreenInfo(newContentRect, newUnlockedVideoSize, newDeviceRotation, mLockedVideoOrientation);
    }

    public static ScreenInfo computeScreenInfo(int rotation, Size deviceSize, Rect crop, int maxSize, int lockedVideoOrientation) {
        if (lockedVideoOrientation == LOCK_VIDEO_ORIENTATION_INITIAL) {
            // The user requested to lock the video orientation to the current orientation
            lockedVideoOrientation = rotation;
        }

        Rect contentRect = new Rect(0, 0, deviceSize.getWidth(), deviceSize.getHeight());
        if (crop != null) {
            if (rotation % 2 != 0) { // 180s preserve dimensions
                // the crop (provided by the user) is expressed in the natural orientation
                crop = flipRect(crop);
            }
            if (!contentRect.intersect(crop)) {
                // intersect() changes contentRect so that it is intersected with crop
                Ln.d(TAG, "Crop rectangle (" + formatCrop(crop) + ") does not intersect device screen (" + formatCrop(deviceSize.toRect()) + ")");
                contentRect = new Rect(); // empty
            }
        }

        Size videoSize = computeVideoSize(contentRect.width(), contentRect.height(), maxSize);
        return new ScreenInfo(contentRect, videoSize, rotation, lockedVideoOrientation);
    }

    private static String formatCrop(Rect rect) {
        return rect.width() + ":" + rect.height() + ":" + rect.left + ":" + rect.top;
    }

    public static Size computeVideoSize(int w, int h, int maxSize) {
        // Compute the video size and the padding of the content inside this video.
        // Principle:
        // - scale down the great side of the screen to maxSize (if necessary);
        // - scale down the other side so that the aspect ratio is preserved;
        // - round this value to the nearest multiple of 8 (H.264 only accepts multiples of 8)
        w &= ~7; // in case it's not a multiple of 8
        h &= ~7;
        if (maxSize > 0) {
            if (BuildConfig.DEBUG && maxSize % 8 != 0) {
                throw new AssertionError("Max size must be a multiple of 8");
            }
            boolean portrait = h > w;
            int major = portrait ? h : w;
            int minor = portrait ? w : h;
            if (major > maxSize) {
                int minorExact = minor * maxSize / major;
                // +4 to round the value to the nearest multiple of 8
                minor = (minorExact + 4) & ~7;
                major = maxSize;
            }
            w = portrait ? minor : major;
            h = portrait ? major : minor;
        }
        return new Size(w, h);
    }

    private static Rect flipRect(Rect crop) {
        return new Rect(crop.top, crop.left, crop.bottom, crop.right);
    }

    /**
     * Return the rotation to apply to the device rotation to get the requested locked video orientation
     *
     * @return the rotation offset
     */
    public int getVideoRotation() {
        if (mLockedVideoOrientation == -1) {
            // no offset
            return 0;
        }
        return (mDeviceRotation + 4 - mLockedVideoOrientation) % 4;
    }

    /**
     * Return the rotation to apply to the requested locked video orientation to get the device rotation
     *
     * @return the (reverse) rotation offset
     */
    public int getReverseVideoRotation() {
        if (mLockedVideoOrientation == -1) {
            // no offset
            return 0;
        }
        return (mLockedVideoOrientation + 4 - mDeviceRotation) % 4;
    }
}
