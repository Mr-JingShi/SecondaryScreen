package com.secondaryscreen.app;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DisplayConnection {
    private static String TAG = "DisplayConnection";
    private Thread mThread;

    public static DisplayConnection getInstance() {
        return DisplayConnection.DisplayConnectionHolder.instance;
    }


    private static class DisplayConnectionHolder {
        private static DisplayConnection instance = new DisplayConnection();
    }

    public DisplayConnection() {
        mThread = new DisplayThread();
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

    class DisplayThread extends Thread {
        DisplayThread() {
            super("DisplayThread");
            Log.d(TAG, "DisplayThread");
        }
        @Override
        public void run() {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(Utils.getRemoteHost(), Utils.DISPLAY_CHANNEL_PORT), Utils.SOCKET_TIMEOUT);
                Log.d(TAG, "DisplayThread connect success");

                byte[] length = new byte[4];
                byte[] bytes = null;
                while (!Thread.currentThread().isInterrupted()) {
                    bytes = Utils.takeDisplayInfoBytes();
                    if (bytes != null) {
                        socket.getOutputStream().write(Utils.intToByte4(bytes.length, length));
                        socket.getOutputStream().write(bytes);
                        socket.getOutputStream().flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "DisplayThread exception:" + e);
            }
        }
    }
}


