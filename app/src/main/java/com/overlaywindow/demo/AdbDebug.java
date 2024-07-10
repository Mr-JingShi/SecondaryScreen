package com.overlaywindow.demo;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;
import io.github.muntashirakon.adb.LocalServices;
import io.github.muntashirakon.adb.android.AdbMdns;
import io.github.muntashirakon.adb.android.AndroidUtils;

public class AdbDebug {
    private static String TAG = "AdbDebug";

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private boolean mConnectSatus = false;
    private int mPort = 0;
    @Nullable
    private AdbStream adbShellStream;
    private Handler mHandler;

    public AdbDebug() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public boolean getConnectStatus() {
        return mConnectSatus;
    }

    public int getPort() {
        return mPort;
    }

    public void connect(int port, Runnable runnable) {
        executor.submit(() -> {
            boolean connectionStatus =false;
            try {
                AbsAdbConnectionManager manager = AdbManager.getInstance(DemoApplication.getApp());
                try {
                    connectionStatus = manager.connect(AndroidUtils.getHostIpAddress(DemoApplication.getApp()), port);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            } catch (Throwable th) {
                connectionStatus = false;
                th.printStackTrace();
            }

            connectResult(mConnectSatus = connectionStatus);
            mHandler.post(runnable);

            Log.i(TAG, "connect connectionStatus: " + connectionStatus);
        });
    }

    public void disconnect() {
        executor.submit(() -> {
            try {
                if (adbShellStream != null) {
                    adbShellStream.close();
                }

                AbsAdbConnectionManager manager = AdbManager.getInstance(DemoApplication.getApp());
                manager.disconnect();
                manager.close();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        });

        executor.shutdown();
        mHandler = null;
    }

    public void getPairingPort(Runnable runnable) {
        executor.submit(() -> {
            AtomicInteger atomicPort = new AtomicInteger(-1);
            CountDownLatch resolveHostAndPort = new CountDownLatch(1);

            AdbMdns adbMdns = new AdbMdns(DemoApplication.getApp(), AdbMdns.SERVICE_TYPE_TLS_PAIRING, (hostAddress, port) -> {
                atomicPort.set(port);
                resolveHostAndPort.countDown();
            });
            adbMdns.start();

            try {
                if (!resolveHostAndPort.await(1, TimeUnit.MINUTES)) {
                    return;
                }
            } catch (InterruptedException ignore) {
            } finally {
                adbMdns.stop();
            }

            mPort = atomicPort.get();
            mHandler.post(runnable);
        });
    }

    public void pair(int port, String pairingCode, Runnable runnable) {
        executor.submit(() -> {
            boolean connected = false;
            try {
                AbsAdbConnectionManager manager = AdbManager.getInstance(DemoApplication.getApp());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    connected = manager.pair(AndroidUtils.getHostIpAddress(DemoApplication.getApp()), port, pairingCode);
                    if (connected) {
                        connected = manager.autoConnect(DemoApplication.getApp(), 5000);
                    }
                }
            } catch (Throwable th) {
                connected = false;
                th.printStackTrace();
            }

            connectResult(mConnectSatus = connected);

            mHandler.post(runnable);

            Log.i(TAG, "pair connected: " + connected);
        });
    }

    public void execute(String command) {
        executor.submit(() -> {
            try {
                if (adbShellStream == null || adbShellStream.isClosed()) {
                    AbsAdbConnectionManager manager = AdbManager.getInstance(DemoApplication.getApp());
                    adbShellStream = manager.openStream(LocalServices.SHELL);
                }
                try (OutputStream os = adbShellStream.openOutputStream()) {
                    os.write(String.format("%1$s\n", command).getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.write("\n".getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void connectResult(boolean success) {
        if (success) {
            StringBuilder sb = new StringBuilder();

            String jarPath = DemoApplication.getApp().getPackageCodePath();
            Log.i(TAG, "jarPath:" + jarPath);

            sb.append("CLASSPATH=");
            sb.append(jarPath);
            sb.append(" ");
            sb.append("nohup app_process / com.secondaryscreen.server.Server ");

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                sb.append(DemoApplication.getApp().getString(R.string.first_activity));
                sb.append(" ");
                sb.append(DemoApplication.getApp().getString(R.string.seoncd_activity));
                sb.append(" ");
            }
            sb.append(">/dev/null 2>&1 &");

            String cmd = sb.toString();
            Log.i(TAG, "connectResult cmd:" + cmd);
            execute(sb.toString());
        }
    }
}