package com.secondaryscreen.server;

import android.os.Looper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class VideoConnection {
    private static int PORT = 8403;
    private Thread mThread;
    public VideoConnection() {}

    public void start() {
        mThread = new VideoServerThread();
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    private class VideoServerThread extends Thread {
        private Thread mThread;
        public VideoServerThread() {
            super("VideoServerThread");
            System.out.println("VideoServerThread");
        }

        @Override
        public void run() {
            try {
                try (ServerSocket serverSocket = new ServerSocket()) {
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(PORT));
                    System.out.println("port:" + PORT);

                    while (true) {
                        Socket socket = serverSocket.accept();
                        System.out.println("VideoServerThread accept");

                        if (mThread != null) {
                            try {
                                mThread.interrupt();
                                mThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        mThread = new VideoSocketThread(socket);
                        mThread.start();
                    }
                }
            } catch (IOException e) {
                System.out.println("VideoServerThread IOException:" + e);
            }
        }
    }

    private class VideoSocketThread extends Thread {
        private Socket mSocket;
        public VideoSocketThread(Socket socket) {
            super("VideoSocketThread");
            System.out.println("VideoSocketThread");
            this.mSocket = socket;
        }

        @Override
        public void run() {
            // Some devices (Meizu) deadlock if the video encoding thread has no Looper
            // <https://github.com/Genymobile/scrcpy/issues/4143>
            Looper.prepare();

            try {
                Streamer videoStreamer = new Streamer(mSocket);
                ScreenCapture screenCapture = new ScreenCapture();
                SurfaceEncoder surfaceEncoder = new SurfaceEncoder(screenCapture, videoStreamer, 8000000, 0, true);
                surfaceEncoder.streamScreen();
            } catch (Exception e) {
                System.out.println("video encoding exception:" + e);
            } finally {
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
