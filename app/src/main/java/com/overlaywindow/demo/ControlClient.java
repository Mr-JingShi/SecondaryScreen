package com.overlaywindow.demo;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;

public class ControlClient {
    private static String TAG = "ControlClient";
    private String HOST = "127.0.0.1";
    private int PORT = 8402;
    private int TIMEOUT = 3000;
    private Thread mThread;
    private boolean mConnected = false;

    public ControlClient() {
        mThread = new ControlClientThread();
    }

    public void setRemoteHost(String remoteHost) {
        this.HOST = remoteHost;
    }

    public boolean getConnected() {
        return mConnected;
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

    class ControlClientThread extends Thread {
        private Socket mSocket;

        ControlClientThread() {
            super("ControlClientThread");
            Log.d(TAG, "ControlClientThread");
        }

        @Override
        public void run() {
            try {
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
                mConnected = true;

                Log.d(TAG, "ControlClientThread connect success");

                byte[] length = new byte[4];
                byte[] bytes = null;
                while (!Thread.currentThread().isInterrupted()) {
                    bytes = Utils.takeBytes();
                    if (bytes != null) {
                        mSocket.getOutputStream().write(Utils.intToByte4(bytes.length, length));
                        mSocket.getOutputStream().write(bytes);
                        mSocket.getOutputStream().flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("socket exception:" + e);
            } finally {
                try {
                    if (mSocket != null) {
                        mSocket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "socket close exception:" + e);
                }
            }
        }
    }
}


