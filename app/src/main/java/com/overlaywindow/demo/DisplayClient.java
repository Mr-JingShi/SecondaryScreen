package com.overlaywindow.demo;

import android.util.Log;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DisplayClient {
    private static String TAG = "VirtualDisplayClient";
    private String HOST = "127.0.0.1";
    private int PORT = 8404;
    private int TIMEOUT = 3000;
    private Thread mThread;
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

        Utils.offerDislayInfoBytes(sb.toString().getBytes());
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

                OutputStream outputStream = socket.getOutputStream();
                byte[] length = new byte[4];
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] bytes = Utils.takeDislayInfoBytes();
                    if (bytes != null) {
                        outputStream.write(Utils.intToByte4(bytes.length, length));
                        outputStream.write(bytes);
                        outputStream.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("DisplayClientThread exception:" + e);
            }
        }
    }
}


