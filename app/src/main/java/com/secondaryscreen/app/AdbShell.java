package com.secondaryscreen.app;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbConnection;
import io.github.muntashirakon.adb.AdbPairingRequiredException;
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
    private int mTlsPort = 0;
    private int mPairPort = 0;
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

    public int getTlsPort() {
        return mTlsPort;
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
                startJar();
            }
            Utils.runOnUiThread(runnable);

            Log.i(TAG, "connect connectionStatus:" + connectionStatus);
        });
    }

    public void autoConnect(Runnable runnable) {
        mExecutor.submit(() -> {
            boolean connected = false;
            try {
                AbsAdbConnectionManager manager = AdbManager.getInstance(Utils.getContext());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    connected = manager.connectTls(Utils.getContext(), 5000);
                    getTlsPort(connected, manager);
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }

            mConnectSatus = connected;
            if (connected) {
                startJar();
            }
            Utils.runOnUiThread(runnable);

            Log.i(TAG, "connect connectionStatus:" + connected);
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
                if (!resolveHostAndPort.await(5, TimeUnit.MINUTES)) {
                    return;
                }
            } catch (InterruptedException ignore) {
            } finally {
                adbMdns.stop();
            }

            mPairPort = atomicPort.get();
            Utils.runOnUiThread(runnable);
        });
    }

    public void pair(String pairingCode, Runnable runnable) {
        mExecutor.submit(() -> {
            boolean connected = false;
            try {
                AbsAdbConnectionManager manager = AdbManager.getInstance(Utils.getContext());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    connected = manager.pair(AndroidUtils.getHostIpAddress(Utils.getContext()), mPairPort, pairingCode);
                    if (connected) {
                        connected = manager.autoConnect(Utils.getContext(), 5000);
                        getTlsPort(connected, manager);
                    }
                }
            } catch (Throwable th) {
                connected = false;
                th.printStackTrace();
            }

            mConnectSatus = connected;
            if (connected) {
                startJar();
            }

            Utils.runOnUiThread(runnable);

            Log.i(TAG, "pair connected:" + connected);
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

    private void getTlsPort(boolean connected, AbsAdbConnectionManager manager) throws Throwable {
        if (connected) {
            AdbConnection adbConnection = manager.getAdbConnection();
            Field field = AdbConnection.class.getDeclaredField("mPort");
            field.setAccessible(true);
            mTlsPort = field.getInt(adbConnection);
            Log.i(TAG, "autoConnect port:" + mTlsPort);
        }
    }

    private void startJar() {
        StringBuilder sb = new StringBuilder();

        String jarPath = Utils.getContext().getPackageCodePath();
        Log.i(TAG, "jarPath:" + jarPath);

        sb.append("CLASSPATH=");
        sb.append(jarPath);
        sb.append(" ");
        sb.append("nohup app_process / com.secondaryscreen.server.Server");

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            sb.append(" ");
            sb.append(Utils.getContext().getString(R.string.first_activity));
            sb.append(" ");
            sb.append(Utils.getContext().getString(R.string.seoncd_activity));
        }
        sb.append(" ");
        sb.append(">/dev/null 2>&1 &");

        String cmd = sb.toString();
        Log.i(TAG, "connectResult cmd:" + cmd);
        execute(cmd);
    }
}