package com.overlaywindow.server;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/DisplayInfo.java

public final class DisplayInfo {
    private final Size mSize;
    private final int mRotation;
    private final int mDensityDpi;
    private int mMirrorDisplayId = 0;

    public DisplayInfo(Size size, int rotation, int densityDpi) {
        this.mSize = size;
        this.mRotation = rotation;
        this.mDensityDpi = densityDpi;
    }

    public Size getSize() {
        return mSize;
    }

    public int getRotation() {
        return mRotation;
    }

    public void setMirrorDisplayId(int displayId) {
        this.mMirrorDisplayId = displayId;
    }
    public int getMirrorDisplayId() {
        return mMirrorDisplayId;
    }

    public int getDensityDpi() {
        return mDensityDpi;
    }
}


