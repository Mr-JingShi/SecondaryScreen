package com.secondaryscreen.server;

import android.os.Build;

public class Server {
    public static String TAG = "Server";
    public static void main(String[] args) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw new RuntimeException("暂不支持Android 10以下设备！！！");
            }

            KillSelf.start();

            DisplayConnection displayConnection = new DisplayConnection();
            displayConnection.start();

            ControlConnection controlConnection = new ControlConnection();
            controlConnection.start();

            controlConnection.join();
            displayConnection.join();
            Utils.shutdown();
        } catch (Exception e) {
            Ln.w(TAG, "Server main exception", e);
        }
    }
}