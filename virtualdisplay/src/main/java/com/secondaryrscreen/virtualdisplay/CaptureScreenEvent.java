package com.secondaryscreen.virtualdisplay;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CaptureScreenEvent {
    private static int PORT = 8404;
    private static String HOST = "127.0.0.1";
    private static List<Socket> mSocketList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            Thread screentEventCaptureThread = new ScreentEventCaptureThread();
            screentEventCaptureThread.start();

            Thread screenEventServerThread = new ScreenEventServerThread();
            screenEventServerThread.start();

            screenEventServerThread.join();
            screentEventCaptureThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ScreentEventCaptureThread extends Thread {
        public ScreentEventCaptureThread() {
            super("ScreentEventCaptureThread");
            System.out.println("ScreentEventCaptureThread");
        }

        @Override
        public void run() {
            String cmd = "getevent -lt";
            Process process = null;
            BufferedReader successResult = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec("sh");
                os = new DataOutputStream(process.getOutputStream());

                os.write(cmd.getBytes());
                os.writeBytes("\n");
                os.flush();

                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));

                byte[] length = new byte[4];
                String line;
                while ((line = successResult.readLine()) != null) {
                    System.out.println(line);

                    if (line.contains("BTN_TOUCH") || line.contains("ABS_MT_POSITION_")) {
                        for (int i = mSocketList.size() - 1; i >= 0; i--) {
                            Socket socket = mSocketList.get(i);

                            byte[] bytes = line.getBytes();

                            try {
                                socket.getOutputStream().write(intToByte4(bytes.length, length));
                                socket.getOutputStream().write(bytes);
                                socket.getOutputStream().flush();
                            } catch (Exception e) {
                                e.printStackTrace();

                                mSocketList.remove(socket);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    if (successResult != null) {
                        successResult.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (process != null) {
                    process.destroy();
                }
            }
        }
    }

    private static class ScreenEventServerThread extends Thread {
        public ScreenEventServerThread() {
            super("ScreenEventServerThread");
            System.out.println("ScreenEventServerThread");
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
                        System.out.println("ScreenEventServerThread accept");

                        mSocketList.add(socket);
                    }
                }
            } catch (IOException e) {
                System.out.println("VideoServerThread IOException:" + e);
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
}