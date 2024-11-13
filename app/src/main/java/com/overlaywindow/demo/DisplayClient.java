package com.overlaywindow.demo;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DisplayClient {
    private static String TAG = "DisplayClient";
    private Thread mThread;
    private Lock mLock = new ReentrantLock();
    private Condition mCondition = mLock.newCondition();
    private String mDisplayInfo;
    public DisplayClient() {
        mThread = new DisplayClientThread();
    }

    public void setScreenInfo(int flag, int width, int height, int densityDpi, int rotation) {
        StringBuilder sb = new StringBuilder();
        sb.append(flag);
        sb.append(",");
        sb.append(width);
        sb.append(",");
        sb.append(height);
        sb.append(",");
        sb.append(densityDpi);
        sb.append(",");
        sb.append(rotation);

        setDisplayInfo(sb.toString());
    }

    private void setDisplayInfo(String displayInfo) {
        mLock.lock();
        try {
            mDisplayInfo = displayInfo;
            mCondition.signal();
        } finally {
            mLock.unlock();
        }

    }

    private String getDisplayInfo() throws InterruptedException {
        String displayInfo = null;
        mLock.lock();
        try {
            while (mDisplayInfo == null) {
                mCondition.await();
            }
            displayInfo = mDisplayInfo;
            mDisplayInfo = null;
        } finally {
            mLock.unlock();
        }
        return displayInfo;
    }

    public void start() {
        mThread.start();
    }

    public void shutdown() {
        try {
            if (mThread != null
                && mThread.isAlive()
                && !mThread.isInterrupted()) {
                mThread.interrupt();
                mThread.join();
                mThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class DisplayClientThread extends Thread {
        DisplayClientThread() {
            super("DisplayClientThread");
            Log.d(TAG, "DisplayClientThread");
        }
        @Override
        public void run() {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(Utils.getRemoteHost(), Utils.DISPLAY_CHANNEL_PORT), Utils.SOCKET_TIMEOUT);
                Log.d(TAG, "DisplayClientThread connect success");

                byte[] length = new byte[4];
                while (!Thread.currentThread().isInterrupted()) {
                    String displayInfo = getDisplayInfo();

                    Log.d(TAG, "displayInfo:" + displayInfo);
                    byte[] bytes = displayInfo.getBytes("UTF-8");
                    if (bytes != null) {
                        socket.getOutputStream().write(Utils.intToByte4(bytes.length, length));
                        socket.getOutputStream().write(bytes);
                        socket.getOutputStream().flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "DisplayClientThread exception:" + e);
            }
        }
    }
}


