package com.overlaywindow.demo;

import android.os.Parcel;
import android.util.Log;
import android.view.MotionEvent;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ControlClient {
    private static String TAG = "ControlClient";
    private static String HOST = "127.0.0.1";
    private static int PORT = 8402;
    private static int TIMEOUT = 3000;

    private ControlClientThread controlClientThread;

    private static final BlockingQueue<MotionEvent> mMotionEventQueue = new ArrayBlockingQueue<>(128);

    public ControlClient() {
        controlClientThread = new ControlClientThread();
        controlClientThread.start();
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
                Log.d(TAG, "host:" + HOST + " port:" + PORT + " timeout:" + TIMEOUT);

                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);

                byte[] length = new byte[4];
                while (!Thread.currentThread().isInterrupted()) {
                    MotionEvent event = mMotionEventQueue.take();
                    Parcel parcel = Parcel.obtain();
                    event.writeToParcel(parcel, 0);
                    byte[] bytes = parcel.marshall();
                    parcel.recycle();

                    mSocket.getOutputStream().write(intToByte4(bytes.length, length));
                    mSocket.getOutputStream().write(bytes);
                    mSocket.getOutputStream().flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("socket exception:" + e);
            }

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

    public static byte[] intToByte4(int i, byte[] targets) {
        targets[3] = (byte) (i & 0xFF);
        targets[2] = (byte) (i >> 8 & 0xFF);
        targets[1] = (byte) (i >> 16 & 0xFF);
        targets[0] = (byte) (i >> 24 & 0xFF);
        return targets;
    }

    public static void offerEvent(MotionEvent event) {
        if (!mMotionEventQueue.offer(event)) {
            Log.d(TAG, "offer error");
        }
    }
}


