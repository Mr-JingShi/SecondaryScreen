package com.overlaywindow.demo;

import android.util.Log;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DisplayClient {
    private static String TAG = "DisplayClient";
    private String HOST = "127.0.0.1";
    private int PORT = 8404;
    private int TIMEOUT = 3000;
    private Thread mThread;
    private Lock mLock = new ReentrantLock();
    private Condition mCondition = mLock.newCondition();
    private String mDisplayInfo;
    public DisplayClient() {
        mThread = new DisplayClientThread();
    }

    public void setRemoteHost(String remoteHost) {
        this.HOST = remoteHost;
    }
    public void setScreenInfo(int flag, int width, int height, int rotation, int densityDpi) {
        StringBuilder sb = new StringBuilder();
        sb.append(flag);
        sb.append(",");
        sb.append(width);
        sb.append(",");
        sb.append(height);
        sb.append(",");
        sb.append(rotation);
        sb.append(",");
        sb.append(densityDpi);

        mLock.lock();
        try {
            mDisplayInfo = sb.toString();
            mCondition.signal();
        } finally {
            mLock.unlock();
        }
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
                socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
                Log.d(TAG, "DisplayClientThread connect success");

                byte[] length = new byte[4];
                while (!Thread.currentThread().isInterrupted()) {
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
                System.out.println("DisplayClientThread exception:" + e);
            }
        }
    }
}


