package com.overlaywindow.demo;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;

public class DisplayClient {
    private static String TAG = "VirtualDisplayClient";
    private String HOST = "127.0.0.1";
    private int PORT = 8404;
    private int TIMEOUT = 3000;
    private Thread mThread;
    private String mSendMessage;
    public DisplayClient() { }

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
        mSendMessage = sb.toString();
    }

    public void start() {
        mThread = new DisplayClientThread();
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
        private Socket mSocket;
        DisplayClientThread() {
            super("DisplayClientThread");
            Log.d(TAG, "DisplayClientThread");
        }
        @Override
        public void run() {
            try {
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);

                Log.d(TAG, "DisplayClientThread connect success");

                byte[] length = new byte[4];
                byte[] bytes = mSendMessage.getBytes();
                Log.i(TAG, "message:" + mSendMessage);

                mSocket.getOutputStream().write(Utils.intToByte4(bytes.length, length));
                mSocket.getOutputStream().write(bytes);
                mSocket.getOutputStream().flush();
                mSocket.close();
                mSocket = null;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("socket exception:" + e);
            }
        }
    }
}


