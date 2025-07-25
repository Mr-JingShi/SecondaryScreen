package com.secondaryscreen.server;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public final class DisplayConnection extends ServerChannel {
    private static String TAG = "DisplayConnection";

    private Runnable mRunnable;
    public DisplayConnection(Runnable runnable) {
       super(Utils.DISPLAY_CHANNEL_PORT);
        mRunnable = runnable;
    }

    @Override
    public void work(byte[] buffer, int length) {
        String displayInfo = new String(buffer, 0, length);
        Ln.d(TAG, "displayInfo:" + displayInfo);
        int displayId = Integer.parseInt(displayInfo);
        DisplayInfo.setDisplayId(displayId);

        mRunnable.run();
    }
}
