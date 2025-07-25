package com.secondaryscreen.app;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DisplayConnection {
    private static String TAG = "DisplayConnection";

    public static void setDisplayInfo(int displayId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(Utils.getRemoteHost(), Utils.DISPLAY_CHANNEL_PORT), Utils.SOCKET_TIMEOUT);
                    Log.d(TAG, "DisplayThread connect success");


                    StringBuilder sb = new StringBuilder();
                    sb.append(displayId);
                    String displayInfo = sb.toString();
                    Log.d(TAG, "displayInfo:" + displayInfo);

                    byte[] length = new byte[4];
                    byte[] bytes = displayInfo.getBytes("UTF-8");
                    if (bytes != null) {
                        socket.getOutputStream().write(Utils.intToByte4(bytes.length, length));
                        socket.getOutputStream().write(bytes);
                        socket.getOutputStream().flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "DisplayThread exception:" + e);
                }
            }
        }).start();
    }
}


