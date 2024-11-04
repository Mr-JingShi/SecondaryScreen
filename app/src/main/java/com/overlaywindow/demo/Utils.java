package com.overlaywindow.demo;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class Utils {
    private static String TAG = "Utils";
    private static Context mContext = null;
    private static boolean mIsSingleMachineMode = true;
    private static int mVirtualDisplayId = -1;
    private static ExecutorService mExecutor  = Executors.newSingleThreadExecutor();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static final BlockingQueue<byte[]> mMotioneventBytesQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<byte[]> mDisplayInfoBytesQueue = new LinkedBlockingQueue<>();

    static void setContext(Context context) {
        mContext = context;
    }
    static Context getContext() {
        return mContext;
    }
    static void setIsSingleMachineMode(boolean isSingleMachineMode) {
        mIsSingleMachineMode = isSingleMachineMode;
    }

    static boolean isSingleMachineMode() {
        return mIsSingleMachineMode;
    }
    static int getVirtualDisplayId() {
        return mVirtualDisplayId;
    }

    static String getWlanAddress() {
        String cmd = "ifconfig | grep 'inet ' | grep -v 127.0.0.1 | awk '{print $2}'";
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    return line.split(":")[1].trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    public static String getHostAddress() {
        InetAddress ip = null;
        try {
            Enumeration<NetworkInterface> en_netInterface = NetworkInterface.getNetworkInterfaces();
            while (en_netInterface.hasMoreElements()) {
                NetworkInterface ni = en_netInterface.nextElement();
                Enumeration<InetAddress> en_ip = ni.getInetAddresses();
                while (en_ip.hasMoreElements()) {
                    ip = en_ip.nextElement();
                    if (!ip.isLoopbackAddress() && !ip.getHostAddress().contains(":")) {
                        break;
                    }
                    else {
                        ip = null;
                    }
                }
                if (ip != null) {
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "getLocalInetAddress exception:" + e.getMessage(), e);
        }

        if (ip == null) {
            Log.e(TAG, "getLocalInetAddress failed");
            throw new RuntimeException("getLocalInetAddress failed");
        }

        return ip.getHostAddress();
    }

    static boolean checkSelfWifi() {
        try {
            String ip = Utils.getHostAddress();
            Log.i(TAG, "getHostAddress:" + ip);
            if (ip == null || ip.isEmpty()) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static boolean checkRemoteWifi(String remoteHost) {
        Future<Boolean> future = mExecutor.submit(() -> {
            try {
                return InetAddress.getByName(remoteHost).isReachable(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        try {
            return future.get().booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static void runOnOtherThread(Runnable runnable) {
        mExecutor.submit(() -> {
            runnable.run();
        });
    }

    static void runOnUiThread(Runnable runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }

    static void toast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    static void startJar() {
        StringBuilder sb = new StringBuilder();

        String jarPath = DemoApplication.getApp().getPackageCodePath();
        Log.i(TAG, "jarPath:" + jarPath);

        sb.append("CLASSPATH=");
        sb.append(jarPath);
        sb.append(" ");
        sb.append("nohup app_process / com.secondaryscreen.server.Server");

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            sb.append(" ");
            sb.append(DemoApplication.getApp().getString(R.string.first_activity));
            sb.append(" ");
            sb.append(DemoApplication.getApp().getString(R.string.seoncd_activity));
        }
        sb.append(" ");
        sb.append(">/dev/null 2>&1 &");

        String cmd = sb.toString();
        Log.i(TAG, "connectResult cmd:" + cmd);
        AdbShell.getInstance().execute(sb.toString());
    }

    static void recvBuffer(InputStream inputStream, byte[] buffer, int sum) throws Exception {
        int read = 0;
        while (sum - read > 0) {
            int len = inputStream.read(buffer, read, sum - read);
            if (len == -1) {
                throw new RuntimeException("socket closed");
            }
            read += len;
        }
    }

    static byte[] intToByte4(int i, byte[] targets) {
        targets[3] = (byte) (i & 0xFF);
        targets[2] = (byte) (i >> 8 & 0xFF);
        targets[1] = (byte) (i >> 16 & 0xFF);
        targets[0] = (byte) (i >> 24 & 0xFF);
        return targets;
    }

     static void offerMotionEvent(MotionEvent event) {
        byte[] bytes = null;
        if (mIsSingleMachineMode) {
            Parcel parcel = Parcel.obtain();
            event.writeToParcel(parcel, 0);
            bytes = parcel.marshall();
            parcel.recycle();
        } else {
            StringBuilder sb = new StringBuilder();

            int action = event.getAction();
            sb.append(action);

            int pointerCount = event.getPointerCount();
            sb.append(";").append(pointerCount);

            for (int i = 0; i < pointerCount; ++i) {
                int pointerId = event.getPointerId(i);
                float x = event.getX(i);
                float y = event.getY(i);
                sb.append(";").append(pointerId).append(",").append(x).append(",").append(y);
            }
            String message = sb.toString();
            bytes = message.getBytes();
        }

        if (bytes != null) {
            offerMotionEventBytes(bytes);
        }
    }

    static void offerMotionEventBytes(byte[] bytes) {
        if (!mMotioneventBytesQueue.offer(bytes)) {
            Log.w(TAG, "offerMotionEventBytes error");
        }
    }

    static byte[] takeMotionEventBytes() throws InterruptedException {
        return mMotioneventBytesQueue.take();
    }

    static void offerDislayInfoBytes(byte[] bytes) {
        if (!mDisplayInfoBytesQueue.offer(bytes)) {
            Log.w(TAG, "offerDislayInfoBytes error");
        }
    }

    static byte[] takeDislayInfoBytes() throws InterruptedException {
        return mDisplayInfoBytesQueue.take();
    }

    static boolean waitVirtualDisplayReady(int count) {
        boolean ready = false;
        for (int i = 0; i < count; i++) {
            ready = checkVirtualDisplayReady();
            if (ready || i == 2) {
                break;
            }
            Log.i(TAG, "serverReady wait 1s");
            Utils.sleep(1000);
        }
        return ready;
    }

    static boolean checkVirtualDisplayReady() {
        DisplayManager dm = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        if (displays.length > 1) {
            for (Display display : displays) {
                if (display.getName().contains("virtualdisplay")) {

                    mVirtualDisplayId = display.getDisplayId();
                    return true;
                }
            }
        }
        return false;
    }
    static void hideKeyboard(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive()) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    static int getRotation() {
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        Log.i(TAG, "rotation:" + rotation);
        return rotation;
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}