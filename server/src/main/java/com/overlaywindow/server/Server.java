package com.overlaywindow.server;

import android.os.Parcel;
import android.view.MotionEvent;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static int PORT = 8402;
    private static String HOST = "127.0.0.1";
    private int mDisplayId = 0;
    private String mMainActivity;
    private String mSecondActivity;

    Event mEvent;

    public Server(String mainActivity, String secondActivity) {
        if (mainActivity != null) {
            mMainActivity = mainActivity;
        }
        if (secondActivity != null) {
            mSecondActivity = secondActivity;
        }

        mEvent = Event.create();

        new DetectThread();

        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(HOST, PORT));
            System.out.println("port:" + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("accept");

                if (mDisplayId != 0) {
                    new ServerThread(socket);
                }
            }
        } catch (Exception e) {
            System.out.println("listen Exception:" + e);
        }
    }

    private int getDisplayId() {
        int displayId = 0;
        String cmd = "dumpsys display | grep OverlayWindowVirtualDisplay";
        Shell.Result sr = Shell.execCommand(cmd, false, true);
        if (sr.result == 0 && sr.successMsg != null && !sr.successMsg.isEmpty()) {
            int begin = sr.successMsg.indexOf("layerStack ");
            int end = sr.successMsg.indexOf(",", begin);
            displayId = Integer.parseInt(sr.successMsg.substring(begin + "layerStack ".length(), end));
        }
        System.out.println("displayId:" + displayId);
        return displayId;
    }

    private boolean activityRunning(String cmd) {
        boolean result = false;
        Shell.Result sr = Shell.execCommand(cmd, false, true);
        if (sr.result == 0 && sr.successMsg != null && !sr.successMsg.isEmpty()) {
            result = true;
        }
        return result;
    }

    private boolean isReady() {
        boolean mainActivityRunning = activityRunning("am stack list | grep " + mMainActivity);
        boolean secondActivityRunning = activityRunning("am stack list | grep " + mSecondActivity);
        System.out.println("mainActivityRunning:" + mainActivityRunning + ",secondActivityRunning:" + secondActivityRunning);
        return mainActivityRunning && !secondActivityRunning;
    }

    public static int byte4ToInt(byte[] bytes, int off) {
        int b0 = bytes[off] & 0xFF;
        int b1 = bytes[off + 1] & 0xFF;
        int b2 = bytes[off + 2] & 0xFF;
        int b3 = bytes[off + 3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    class ServerThread extends Thread {
        private Socket mSocket;
        public ServerThread(Socket s) {
            super("ServerThread");
            System.out.println("ServerThread");
            mSocket = s;
            start();
        }

        @Override
        public void run() {
            try {
                byte[] bytes = new byte[2048];
                byte[] length = new byte[4];
                int len = 0;
                InputStream inputStream = mSocket.getInputStream();
                while (!Thread.currentThread().isInterrupted()) {
                    len = inputStream.read(length, 0, length.length);

                    len = inputStream.read(bytes, 0, byte4ToInt(length, 0));

                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(bytes, 0, len);
                    parcel.setDataPosition(0);
                    MotionEvent event = MotionEvent.CREATOR.createFromParcel(parcel);
                    parcel.recycle();

                    Event.setDisplayId(event, mDisplayId);
                    mEvent.injectInputEvent(event, Event.INJECT_INPUT_EVENT_MODE_ASYNC);
                }
            } catch (Exception e) {
                System.out.println("socket error:" + e.toString());
            }

            try {
                mSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class DetectThread extends Thread {
        DetectThread() {
            super("DetectThread");
            System.out.println("DetectThread");
            start();
        }
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isReady()) {
                        Thread.sleep(2000);

                        if (isReady()) {
                            mDisplayId = getDisplayId();
                            if (mDisplayId != 0) {
                                StringBuilder sb = new StringBuilder();

                                sb.append("am start -n ");
                                sb.append(mSecondActivity);
                                sb.append(" --display ");
                                sb.append(mDisplayId);

                                String text = sb.toString();
                                Shell.Result sr = Shell.execCommand(text, false, true);
                                if (sr.result == 0) {
                                    System.out.println("am start secondActivity success");
                                }
                            }
                        }
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("socket error：" + e.toString());
                }
            }
        }
    }
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("args.length != 2");
            return;
        }

        System.out.println("mainActivity:" + args[0]);
        System.out.println("secondActivity:" + args[1]);

        new Server(args[0], args[1]);
    }
}