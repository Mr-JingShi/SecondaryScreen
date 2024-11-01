package com.secondaryscreen.server;

import android.view.IRotationWatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class DisplayConnection {
    private static String TAG = "DisplayConnection";
    private static int PORT = 8404;
    private Thread mThread;
    private Socket mSocket;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private int mDensityDpi;
    public DisplayConnection(int width, int height, int rotation, int densityDpi) {
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
        mDensityDpi = densityDpi;

        ServiceManager.getWindowManager().registerRotationWatcher(new IRotationWatcher.Stub() {
            @Override
            public void onRotationChanged(int rotation) {
                System.out.println("onRotationChanged rotation:" + rotation);

                synchronized (Utils.class) {
                    if (Utils.isSingleMachineMode()) {
                        ServiceManager.getDisplayManager().getDisplayInfo(true);

                        changeVirtualDisplayRotation(rotation);
                    }
                }
            }
        }, 0);
    }

    private void changeVirtualDisplayRotation(int rotation) {
        int displayId = DisplayInfo.getMirrorDisplayId();

        if (!ServiceManager.getWindowManager().isRotationFrozen(displayId)) {
            ServiceManager.getWindowManager().thawRotation(displayId);
        }

        ServiceManager.getWindowManager().freezeRotation(displayId, rotation);

        ServiceManager.getWindowManager().onRotationChanged(rotation);
    }

    private void resizeVirtualDisplay(int width, int height, int densityDpi) throws Exception {
        SurfaceControl.resizeVirtualDisplay(width, height, densityDpi);
    }

    public void start() {
        mThread = new DisplayServerThread();
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    private class DisplayServerThread extends Thread {
        Thread mThread;
        public DisplayServerThread() {
            super("DisplayServerThread");
            System.out.println("DisplayServerThread");
        }

        @Override
        public void run() {
            try {
                try (ServerSocket serverSocket = new ServerSocket()) {
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(PORT));

                    while (true) {
                        Socket socket = serverSocket.accept();
                        System.out.println("DisplayServerThread accept");

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
                        mThread = new DisplaySocketThread();
                        mThread.start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("DisplayServerThread IOException:" + e);
            }
        }
    }

    private class DisplaySocketThread extends Thread {
        public DisplaySocketThread() {
            super("DisplaySocketThread");
            System.out.println("DisplaySocketThread");
        }

        @Override
        public void run() {
            try {
                byte[] eventBuffer = new byte[0];
                byte[] lengthBuffer = new byte[4];
                int len = 0;
                InputStream inputStream = mSocket.getInputStream();
                Utils.recv(inputStream, lengthBuffer, lengthBuffer.length);

                len = Utils.byte4ToInt(lengthBuffer);
                if (eventBuffer.length < len) {
                    System.out.println("eventBuffer.length:" + eventBuffer.length + " < len:" + len);
                    eventBuffer = new byte[len];
                }
                Utils.recv(inputStream, eventBuffer, len);

                // 1200,1920,240
                String buffer = new String(eventBuffer);
                System.out.println("buffer:" + buffer);
                String[] split = buffer.split(",");
                int flag = Integer.parseInt(split[0]);
                int width = Integer.parseInt(split[1]);
                int height = Integer.parseInt(split[2]);
                int rotation = Integer.parseInt(split[3]);
                int densityDpi = Integer.parseInt(split[4]);

                // 0:Local 1:Remote
                Utils.setSingleMachineMode(flag == 0);

                System.out.println("old width:" + mWidth + " height:" + mHeight + " rotation:" + mRotation + " densityDpi:" + mDensityDpi);
                System.out.println("new width:" + width + " height:" + height + " rotation:" + rotation + " densityDpi:" + densityDpi);

                boolean changed = false;
                if (mWidth != width || mHeight != height || mDensityDpi != densityDpi) {
                    mWidth = width;
                    mHeight = height;
                    mDensityDpi = densityDpi;

                    resizeVirtualDisplay(width, height, densityDpi);
                    changed = true;
                }
                if (mRotation != rotation) {
                    mRotation = rotation;

                    changeVirtualDisplayRotation(rotation);
                    changed = true;
                }
                if (changed) {
                    if (mRotation % 2 == 1) {
                        ServiceManager.getDisplayManager().forceDisplayInfo(mHeight, mWidth, mRotation, mDensityDpi);
                    } else {
                        ServiceManager.getDisplayManager().forceDisplayInfo(mWidth, mHeight, mRotation, mDensityDpi);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("DisplaySocketThread exception:" + e);
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
}
