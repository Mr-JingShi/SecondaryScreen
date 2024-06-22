package com.secondaryscreen.server;

import android.graphics.Rect;

import java.util.Objects;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/Size.java

public final class Size {
    private final int mWidth;
    private final int mHeight;

    public Size(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public Size rotate() {
        return new Size(mHeight, mWidth);
    }

    public Rect toRect() {
        return new Rect(0, 0, mWidth, mHeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Size size = (Size) o;
        return mWidth == size.mWidth && mHeight == size.mHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mWidth, mHeight);
    }

    @Override
    public String toString() {
        return "Size{" + "width=" + mWidth + ", height=" + mHeight + '}';
    }
}
