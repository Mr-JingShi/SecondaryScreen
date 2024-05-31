package com.overlaywindow.demo;

import android.os.Parcel;
import android.util.Log;
import android.view.MotionEvent;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class SocketClient {
    private static String TAG = "SocketClient";
    private static String HOST = "127.0.0.1";
    private static int PORT = 8402;
    private static int TIMEOUT = 3000;
    private Socket mSocket;
    private OutputStream mOutputStream;

    private SendThread mSendThread;

    private static final BlockingQueue<MotionEvent> mMotionEventQueue = new ArrayBlockingQueue<>(128);

    public SocketClient() {
        mSendThread = new SendThread();
    }
    class SendThread extends Thread {
        SendThread() {
            super("SendThread");
            System.out.println("SendThread");
            start();
        }
        @Override
        public void run() {
            try {
                Log.d(TAG, "host:" + HOST + " port:" + PORT + " timeout:" + TIMEOUT);

                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);

                mSocket.setSoTimeout(TIMEOUT);

                mOutputStream = mSocket.getOutputStream();

                byte[] length = new byte[4];
                while (!Thread.currentThread().isInterrupted()) {
                    MotionEvent event = mMotionEventQueue.take();
                    Parcel parcel = Parcel.obtain();
                    event.writeToParcel(parcel, 0);
                    byte[] bytes = parcel.marshall();
                    parcel.recycle();

                    mOutputStream.write(intToByte4(bytes.length, length));
                    mOutputStream.write(bytes);
                    mOutputStream.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("socket error:" + e.toString());
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

    public static void send(MotionEvent event) {
        if (!mMotionEventQueue.offer(event)) {
            Log.d(TAG, "offer error");
        }
    }

    public void close() {
        try {
            mOutputStream.close();
            mSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "send error:" + e);
        }
    }

    public void stop() {
        if (mSendThread != null) {
            mSendThread.interrupt();
        }
    }

    public void join() {
        try {
            if (mSendThread != null) {
                mSendThread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "join error:" + e);
        }
    }
}


