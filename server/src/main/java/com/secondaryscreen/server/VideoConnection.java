package com.secondaryscreen.server;

import android.os.Looper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class VideoConnection {
    private static int PORT = 8403;
    private static String HOST = "127.0.0.1";
    private DisplayInfo mDisplayInfo;
    private Thread mThread;
    public VideoConnection(DisplayInfo displayInfo) {
        this.mDisplayInfo = displayInfo;
    }

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
        public VideoServerThread() {
            super("VideoServerThread");
            System.out.println("VideoServerThread");
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
                        System.out.println("VideoSocketThread accept");

                        Thread thread = new VideoSocketThread(socket);
                        thread.start();
                    }
                }
            } catch (IOException e) {
                System.out.println("VideoServerThread IOException:" + e);
            }
        }
    }

    private class VideoSocketThread extends Thread {
        private Socket socket;
        public VideoSocketThread(Socket socket) {
            super("VideoSocketThread");
            System.out.println("VideoSocketThread");
            this.socket = socket;
        }

        @Override
        public void run() {
            // Some devices (Meizu) deadlock if the video encoding thread has no Looper
            // <https://github.com/Genymobile/scrcpy/issues/4143>
            Looper.prepare();

            try {
                Streamer videoStreamer = new Streamer(socket);
                ScreenCapture screenCapture = new ScreenCapture(mDisplayInfo);
                SurfaceEncoder surfaceEncoder = new SurfaceEncoder(screenCapture, videoStreamer, 8000000, 0, true);
                surfaceEncoder.streamScreen();
            } catch (Exception e) {
                System.out.println("video encoding exception:" + e);
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
