package com.secondaryscreen.server;

import android.os.Parcel;
import android.util.Log;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class ControlConnection {
    private static int PORT = 8402;
    private static String HOST = "127.0.0.1";
    private int mDisplayId;
    private Thread mThread;
    public ControlConnection(int displayId) {
        this.mDisplayId = displayId;
    }

    public void start() {
        mThread = new ContrrolServerThread();
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }


    private class ContrrolServerThread extends Thread {
        public ContrrolServerThread() {
            super("ContrrolServerThread");
            System.out.println("ContrrolServerThread");
        }

        @Override
        public void run() {
            try {
                try (ServerSocket serverSocket = new ServerSocket()) {
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(HOST, PORT));
                    System.out.println("port:" + PORT);

                    while (true) {
                        Socket socket = serverSocket.accept();
                        System.out.println("controlSocket accept");

                        Thread thread = new ControlSocketThread(socket);
                        thread.start();
                    }
                }
            } catch (IOException e) {
                System.out.println("VideoServerThread IOException:" + e);
            }
        }
    }

    private class ControlSocketThread extends Thread {
        private Socket mSocket;
        public ControlSocketThread(Socket socket) {
            super("ControlSocketThread");
            System.out.println("ControlSocketThread");
            this.mSocket = socket;
        }

        @Override
        public void run() {
            try {
                byte[] eventBuffer = new byte[0];
                byte[] lengthBuffer = new byte[4];
                int len = 0;
                InputStream inputStream = mSocket.getInputStream();
                while (!Thread.currentThread().isInterrupted()) {
                    recv(inputStream, lengthBuffer, lengthBuffer.length);

                    len = byte4ToInt(lengthBuffer);
                    if (eventBuffer.length < len) {
                        System.out.println("eventBuffer.length:" + eventBuffer.length + " < len:" + len);
                        eventBuffer = new byte[len];
                    }
                    recv(inputStream, eventBuffer, len);

                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(eventBuffer, 0, len);
                    parcel.setDataPosition(0);
                    MotionEvent event = MotionEvent.CREATOR.createFromParcel(parcel);
                    parcel.recycle();

                    InputManager.setDisplayId(event, mDisplayId);
                    ServiceManager.getInputManager().injectInputEvent(event);
                }
            } catch (Exception e) {
                System.out.println("control socket exception:" + e);
            }  finally {
                try {
                    if (mSocket != null) {
                        mSocket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void recv(InputStream inputStream, byte[] buffer, int sum) throws Exception {
        int read = 0;
        while (sum - read > 0) {
            int len = inputStream.read(buffer, read, sum - read);
            if (len == -1) {
                throw new RuntimeException("socket closed");
            }
            read += len;
        }
    }

    public static int byte4ToInt(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
}
