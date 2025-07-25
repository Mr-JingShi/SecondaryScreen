package com.secondaryscreen.server;

// 部分逻辑参考自：
// https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/DisplayInfo.java

public final class DisplayInfo {
    private static int mDisplayId = 0;
    public static void setDisplayId(int displayId) {
        mDisplayId = displayId;
    }
    public static int getDisplayId() {
        return mDisplayId;
    }

}


