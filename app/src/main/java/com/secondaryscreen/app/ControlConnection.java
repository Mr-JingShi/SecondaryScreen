package com.secondaryscreen.app;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;

public class ControlConnection {
    private static String TAG = "ControlConnection";
    private Thread mThread;

    public ControlConnection() {
        mThread = new ControlThread();
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

    class ControlThread extends Thread {
        ControlThread() {
            super("ControlThread");
            Log.d(TAG, "ControlThread");
        }

        @Override
        public void run() {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(Utils.getRemoteHost(), Utils.CONTROL_CHANNEL_PORT), Utils.SOCKET_TIMEOUT);

                Log.d(TAG, "ControlThread connect success");

                byte[] length = new byte[4];
                byte[] bytes = null;
                while (!Thread.currentThread().isInterrupted()) {
                    bytes = Utils.takeMotionEventBytes();
                    if (bytes != null) {
                        socket.getOutputStream().write(Utils.intToByte4(bytes.length, length));
                        socket.getOutputStream().write(bytes);
                        socket.getOutputStream().flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "ControlThread socket exception:" + e);
            }
        }
    }
}


