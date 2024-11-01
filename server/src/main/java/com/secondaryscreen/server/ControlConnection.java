package com.secondaryscreen.server;

import android.os.Parcel;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class ControlConnection {
    private static int PORT = 8402;
    private Thread mThread;
    private Socket mSocket;
    public ControlConnection() {

    }

    public void start() {
        mThread = new ControlServerThread();
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    private class ControlServerThread extends Thread {
        private Thread mThread;
        public ControlServerThread() {
            super("ControlServerThread");
            System.out.println("ControlServerThread");
        }

        @Override
        public void run() {
            try {
                try (ServerSocket serverSocket = new ServerSocket()) {
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(PORT));

                    while (true) {
                        Socket socket = serverSocket.accept();
                        System.out.println("ControlServerThread accept");

                        try {
                            if (mSocket != null && !mSocket.isClosed()) {
                                mSocket.close();
                            }
                            if (mThread != null && !mThread.isInterrupted()) {
                                mThread.interrupt();
                                mThread.join();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mSocket = socket;
                        mThread = new ControlSocketThread(socket);
                        mThread.start();
                    }
                }
            } catch (IOException e) {
                System.out.println("VideoServerThread IOException:" + e);
            }
        }
    }

    private class ControlSocketThread extends Thread {
        private long lastTouchDown;
        private MotionEvent.PointerProperties[] pointerProperties;
        private MotionEvent.PointerCoords[] pointerCoords;
        public ControlSocketThread(Socket socket) {
            super("ControlSocketThread");
            System.out.println("ControlSocketThread");
        }

        @Override
        public void run() {
            try {
                byte[] eventBuffer = new byte[0];
                byte[] lengthBuffer = new byte[4];
                int len = 0;
                InputStream inputStream = mSocket.getInputStream();
                while (!Thread.currentThread().isInterrupted()) {
                    Utils.recv(inputStream, lengthBuffer, lengthBuffer.length);

                    len = Utils.byte4ToInt(lengthBuffer);
                    if (eventBuffer.length < len) {
                        System.out.println("eventBuffer.length:" + eventBuffer.length + " < len:" + len);
                        eventBuffer = new byte[len];
                    }
                    Utils.recv(inputStream, eventBuffer, len);

                    MotionEvent event = null;
                    if (Utils.isSingleMachineMode()) {
                        Parcel parcel = Parcel.obtain();
                        parcel.unmarshall(eventBuffer, 0, len);
                        parcel.setDataPosition(0);
                        event = MotionEvent.CREATOR.createFromParcel(parcel);
                        parcel.recycle();
                    } else {
                        initMotionEvent();
                        String eventMessage = new String(eventBuffer, 0, len);
                        event = createMotionEvent(eventMessage);
                    }

                    if (event != null) {
                        InputManager.setDisplayId(event, DisplayInfo.getMirrorDisplayId());
                        ServiceManager.getInputManager().injectInputEvent(event);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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

        private void initMotionEvent() {
            if (pointerProperties == null || pointerCoords == null) {
                pointerProperties = new MotionEvent.PointerProperties[10];
                pointerCoords = new MotionEvent.PointerCoords[10];
                for (int i = 0; i < 10; i++) {
                    pointerProperties[i] = new MotionEvent.PointerProperties();
                    pointerProperties[i].id = i;
                    pointerProperties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
                    pointerCoords[i] = new MotionEvent.PointerCoords();
                    pointerCoords[i].x = 0;
                    pointerCoords[i].y = 0;
                    pointerCoords[i].pressure = 1.0f;
                    pointerCoords[i].size = 1.0f;
                }
            }
        }

        private MotionEvent createMotionEvent(String eventMessage) {
            String[] splited = eventMessage.split(";");
            int action = Integer.parseInt(splited[0]);
            int pointerCount = Integer.parseInt(splited[1]);
            for (int i = 0; i < pointerCount; i++) {
                String[] pointers = splited[i + 2].split(",");
                pointerProperties[i].id = Integer.parseInt(pointers[0]);
                pointerCoords[i].x = Float.parseFloat(pointers[1]);
                pointerCoords[i].y = Float.parseFloat(pointers[2]);
                pointerCoords[i].pressure = action == MotionEvent.ACTION_UP ? 0.0f : 1.0f;
            }
            long now = SystemClock.uptimeMillis();
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }

            MotionEvent event = MotionEvent.obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            return event;
        }
    }
}
