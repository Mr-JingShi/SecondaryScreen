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

// 部分逻辑参考自：
// https://github.com/MuntashirAkon/libadb-android/blob/master/app/src/main/java/io/github/muntashirakon/adb/testapp/MainViewModel.java

public class AdbShell {
    private static class Hodler {
        private static AdbShell hodler = new AdbShell();
    }
    private static String TAG = "AdbShell";
    private final ExecutorService mExecutor;
    private boolean mConnectSatus = false;
    private int mPort = 0;
    @Nullable
    private AdbStream adbShellStream;

    public AdbShell() {
        mExecutor = Executors.newFixedThreadPool(3);
    }

    public static AdbShell getInstance() {
        return Hodler.hodler;
    }

    public boolean getConnectStatus() {
        return mConnectSatus;
    }

    public int getPort() {
        return mPort;
    }

    public void connect(int port, Runnable runnable) {
        mExecutor.submit(() -> {
            boolean connectionStatus =false;
            try {
                AbsAdbConnectionManager manager = AdbManager.getInstance(Utils.getContext());
                try {
                    connectionStatus = manager.connect(AndroidUtils.getHostIpAddress(Utils.getContext()), port);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            } catch (Throwable th) {
                connectionStatus = false;
                th.printStackTrace();
            }

            mConnectSatus = connectionStatus;
            if (connectionStatus) {
                Utils.startJar();
            }
            Utils.runOnUiThread(runnable);

            Log.i(TAG, "connect connectionStatus: " + connectionStatus);
        });
    }

    public void disconnect() {
        mExecutor.submit(() -> {
            try {
                if (adbShellStream != null) {
                    adbShellStream.close();
                }

                AbsAdbConnectionManager manager = AdbManager.getInstance(Utils.getContext());
                manager.disconnect();
                manager.close();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        });

        mExecutor.shutdown();
    }

    public void getPairingPort(Runnable runnable) {
        mExecutor.submit(() -> {
            AtomicInteger atomicPort = new AtomicInteger(-1);
            CountDownLatch resolveHostAndPort = new CountDownLatch(1);

            AdbMdns adbMdns = new AdbMdns(Utils.getContext(), AdbMdns.SERVICE_TYPE_TLS_PAIRING, (hostAddress, port) -> {
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
            Utils.runOnUiThread(runnable);
        });
    }

    public void pair(int port, String pairingCode, Runnable runnable) {
        mExecutor.submit(() -> {
            boolean connected = false;
            try {
                AbsAdbConnectionManager manager = AdbManager.getInstance(Utils.getContext());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    connected = manager.pair(AndroidUtils.getHostIpAddress(Utils.getContext()), port, pairingCode);
                    if (connected) {
                        connected = manager.autoConnect(Utils.getContext(), 5000);
                    }
                }
            } catch (Throwable th) {
                connected = false;
                th.printStackTrace();
            }

            mConnectSatus = connected;
            if (connected) {
                Utils.startJar();
            }

            Utils.runOnUiThread(runnable);

            Log.i(TAG, "pair connected: " + connected);
        });
    }

    public void execute(String command) {
        mExecutor.submit(() -> {
            try {
                if (adbShellStream == null || adbShellStream.isClosed()) {
                    AbsAdbConnectionManager manager = AdbManager.getInstance(Utils.getContext());
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
}