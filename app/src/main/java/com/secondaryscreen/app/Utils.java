package com.secondaryscreen.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
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
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class Utils {
    private static String TAG = "Utils";
    static final String VIRTUALDISPLAY_NAME = "secondaryscreen";
    static int CONTROL_CHANNEL_PORT = 8402;
    static int DISPLAY_CHANNEL_PORT = 8404;
    static int SOCKET_TIMEOUT = 3000;
    private static String REMOTE_HOST = "127.0.0.1";
    private static Context mContext = null;
    private static ExecutorService mExecutor  = Executors.newSingleThreadExecutor();
    private static final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private static final BlockingQueue<byte[]> mMotioneventBytesQueue = new LinkedBlockingQueue<>();
    static void setContext(Context context) {
        mContext = context;
    }
    static Context getContext() {
        return mContext;
    }

    static String getServiceAdbTcpPort() {
        String port = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            port = (String)(get.invoke(c, "service.adb.tcp.port", port));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return port;
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
            mMainHandler.post(runnable);
        }
    }

    static void toast(String msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }

    static void toast(String msg, int duration) {
        Toast.makeText(mContext, msg, duration).show();
    }

    static byte[] intToByte4(int i, byte[] targets) {
        targets[3] = (byte) (i & 0xFF);
        targets[2] = (byte) (i >> 8 & 0xFF);
        targets[1] = (byte) (i >> 16 & 0xFF);
        targets[0] = (byte) (i >> 24 & 0xFF);
        return targets;
    }

     static void offerMotionEvent(MotionEvent event) {
         Parcel parcel = Parcel.obtain();
         event.writeToParcel(parcel, 0);
         byte[] bytes = parcel.marshall();
         parcel.recycle();
         offerMotionEventBytes(bytes);
    }

    static void offerMotionEventBytes(byte[] bytes) {
        if (!mMotioneventBytesQueue.offer(bytes)) {
            Log.w(TAG, "offerMotionEventBytes error");
        }
    }

    static byte[] takeMotionEventBytes() throws InterruptedException {
        return mMotioneventBytesQueue.take();
    }

    static boolean checkJarReady() {
        Future<Boolean> future = mExecutor.submit(() -> {
            try (Socket socket = new Socket()) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(CONTROL_CHANNEL_PORT));
            } catch (java.net.BindException e) {
                Log.i(TAG, "BindException exception:" + e);
                String message = e.getMessage();
                if (message.contains("EADDRINUSE") || message.contains("Address already in use")) {
                    return true;
                }
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

    static int getRotation() {
        // WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        // int rotation = wm.getDefaultDisplay().getRotation();

        // 在APP侧创建virtualdisplay时，会扰乱Display信息，比如getDefaultDisplay会返回virutaldisplay的而不是默认屏幕的
        // 且dispalyId=0的display在获取rotation时返回的也是virutaldisplay的而不是默认屏幕的
        // 所以此次使用解析display.toString()的方式获取rotation
        DisplayManager dm = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        for (Display display : displays) {
            if (display.getDisplayId() == 0) {
                String str = display.toString();
                Log.i(TAG, "str:" + str);
                int index1 = str.indexOf("rotation ");
                int index2 = str.indexOf(",", index1);
                int rotation = Integer.parseInt(str.substring(index1 + 9, index2));
                Log.i(TAG, "rotation:" + rotation);
                return rotation;
            }
        }
        throw new RuntimeException("getRotation error");
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (IllegalArgumentException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static String getRemoteHost() {
        return REMOTE_HOST;
    }

    static void finishAndRemoveTask(String className) {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();

        for (ActivityManager.AppTask appTask : appTasks) {
            ActivityManager.RecentTaskInfo recentTaskInfo = appTask.getTaskInfo();
            Log.i(TAG, "recentTaskInfo:" + recentTaskInfo);
            Intent baseIntent = recentTaskInfo.baseIntent;
            if (baseIntent.getComponent() != null
                && baseIntent.getComponent().getClassName().equals(className)) {
                Log.i(TAG, "setExcludeFromRecents:" + className);
                appTask.setExcludeFromRecents(true);
                appTask.finishAndRemoveTask();
            }
        }
    }
}
