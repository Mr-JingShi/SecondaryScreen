package com.overlaywindow.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static String TAG = "MainActivity";
    private static int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private static int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;
    private static FloatWindow mFloatWindow;
    private ImageView mTcpipImageView;
    private ImageView mPairImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MainActivity onCreate");
        super.onCreate(savedInstanceState);

        if (mFloatWindow != null) {
            Utils.toast("检测到悬浮窗在运行，请先关闭悬浮窗！");
            finish();
        }

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        mTcpipImageView = findViewById(R.id.adb_tcpip);
        mTcpipImageView.setOnClickListener((View v) -> {
            adbTcpipConnect();
        });

        mPairImageView = findViewById(R.id.adb_pair);
        mPairImageView.setOnClickListener((View v) -> {
            adbPairConnect();
        });

        Spinner spinner = findViewById(R.id.spinner);
        RadioButton radio_master = findViewById(R.id.radio_master);
        RadioButton radio_slave = findViewById(R.id.radio_slave);
        TextView wlan_description = findViewById(R.id.wlan_description);
        TextView wlan_address = findViewById(R.id.wlan_address);
        String remoteHost = PrivatePreferences.getRemoteHost();
        wlan_address.setText(remoteHost);
        chooseResolution(spinner);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            radio_master.setVisibility(View.GONE);
            radio_slave.setChecked(true);
            wlan_description.setVisibility(View.VISIBLE);
            wlan_address.setVisibility(View.VISIBLE);
        } else {
            radio_master.setOnClickListener((view) -> {
                spinner.setVisibility(View.GONE);
                String ip = Utils.getHostAddress();
                if (ip.isEmpty()) {
                    wlan_description.setVisibility(View.GONE);
                } else {
                    wlan_description.setVisibility(View.VISIBLE);
                    wlan_description.setText("本设备WLAN地址：\n" + ip);
                }
                wlan_address.setVisibility(View.GONE);
            });
            radio_slave.setOnClickListener((view) -> {
                spinner.setVisibility(View.VISIBLE);
                wlan_description.setVisibility(View.VISIBLE);
                wlan_description.setText("主设备WLAN地址：");
                wlan_address.setVisibility(View.VISIBLE);
                setAdbConnectionVisibility(false);
            });
        }

        findViewById(R.id.start_button).setOnClickListener((view) -> {
            if (radio_master.isChecked()) {
                if (checkVirtualDisplayReady()) {
                    startFloatWindow();
                } else {
                    Utils.toast("jar包未启动，请先启动jar包");
                }
            } else if (radio_slave.isChecked()) {
                CharSequence charSequence = wlan_address.getText();
                if (charSequence != null) {
                    String wlanAddress = charSequence.toString();
                    if (wlanAddress.isEmpty()) {
                        Utils.toast("请输入主设备WLAN地址");
                        return;
                    }

                    if (!Utils.checkRemoteWifi(wlanAddress)) {
                        Utils.toast("请输入正确的主设备WLAN地址");
                        return;
                    }

                    if (wlanAddress.equals("127.0.0.1")) {
                        if (!checkVirtualDisplayReady()) {
                            Utils.toast("jar包未启动，请先启动jar包");
                            return;
                        }
                    }

                    PrivatePreferences.setRemoteHost(wlanAddress);
                    Utils.setRemoteHost(wlanAddress);

                    Utils.hideKeyboard(view);

                    Intent intent = new Intent(this, SecondaryScreenActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (hasOverlayPermission()) {
                showFloatWindow();
            } else {
                Utils.toast("请授予悬浮窗权限", Toast.LENGTH_LONG);
            }
            finish();
        } else if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            Log.i(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode + " data:" + data);
            if (hasNotificationPermission()) {
                Utils.toast("Notification permission was granted.");

                mPairImageView.setVisibility(View.VISIBLE);
            } else {
                Utils.toast("Notification permission request was denied.", Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String connected = intent.getStringExtra("connected");
            if (connected != null) {
                Utils.toast("WLAN-ADB调试配对结果:" + connected);

                if (connected.equals("OK") && Utils.waitVirtualDisplayReady(3)) {
                    setAdbConnectionVisibility(false);

                    AdbShell.getInstance().disconnect();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startFloatWindow() {
        if (hasOverlayPermission()) {
            showFloatWindow();

            finish();
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        }
    }

    private void showFloatWindow() {
        Log.i(TAG, "showFloatWindow");
        mFloatWindow = new FloatWindow();
        mFloatWindow.show();
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private void chooseResolution(Spinner spinner) {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        if (display.getRotation() % 2 == 0) {
            Resolution selfResolution = new Resolution("self",
                    realMetrics.widthPixels,
                    realMetrics.heightPixels,
                    realMetrics.densityDpi,
                    metrics.widthPixels,
                    metrics.heightPixels);
            chooseResolution(spinner, selfResolution);
        } else {
            Resolution selfResolution = new Resolution("self",
                    realMetrics.heightPixels,
                    realMetrics.widthPixels,
                    realMetrics.densityDpi,
                    metrics.heightPixels,
                    metrics.widthPixels);
            chooseResolution(spinner, selfResolution);
        }
    }

    private void chooseResolution(Spinner spinner, Resolution selfResolution) {
        Log.d(TAG, "chooseResolution selfResolution:" + selfResolution);

        ArrayList<Resolution> resolutions = new ArrayList<>();
        resolutions.add(new Resolution("480p", 480, 720, 142));
        resolutions.add(new Resolution("720p", 720, 1280, 213));
        resolutions.add(new Resolution("1080p", 1080, 1920, 320));
        resolutions.add(new Resolution("4k", 2160, 3840, 320));

        float[] scales = {2.0f, 1.5f, 1.0f, 0.5f};
        ArrayList<String> resolutionStrings = new ArrayList<>();
        for (int i = resolutions.size() - 1 ; i >= 0; i--) {
            Resolution resolution = resolutions.get(i);
            boolean match = false;
            for (int j = 0; j < scales.length; j++) {
                if (selfResolution.TEXTUREVIEW_WIDTH >= resolution.TEXTUREVIEW_WIDTH * scales[j]
                    && selfResolution.TEXTUREVIEW_HEIGHT >= resolution.TEXTUREVIEW_HEIGHT * scales[j]) {
                    resolution.changeScale(scales[j], scales[j]);

                    Log.d(TAG, "chooseResolution RESOLUTIONS[" + i + "]:" + resolution);
                    resolutionStrings.add(resolution.toSimpleString());
                    match = true;
                    break;
                }
            }
            if (!match) {
                resolutions.remove(i);
            }
        }

        if (!resolutions.isEmpty()) {
            Collections.reverse(resolutionStrings);
        }

        boolean found = false;
        for (Resolution resolution : resolutions) {
            if (resolution.match(selfResolution)) {
                found = true;
                break;
            }
        }
        if (!found) {
            resolutions.add(selfResolution);
            resolutionStrings.add(selfResolution.toSimpleString());
        }

        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, resolutionStrings));

        String resolution = PrivatePreferences.getResolution();
        Log.i(TAG, "resolution:" + resolution);
        for (int i = 0; i < resolutionStrings.size(); i++) {
            if (resolutionStrings.get(i).equals(resolution)) {
                spinner.setSelection(i, true);

                Resolution.R = resolutions.get(i);
                break;
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = (String) adapterView.getItemAtPosition(i);
                Utils.toast(item);

                Log.i(TAG, "i:" + i + " item:" + item + " resolutions:" + resolutions.get(i).toString());

                Resolution.R = resolutions.get(i);

                PrivatePreferences.setResolution(item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    public boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public void requestNotificationsPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATION_PERMISSION);
        } else {
            if (!PrivatePreferences.getNotificationPermissionRequested()) {
                PrivatePreferences.setNotificationPermissionRequested(true);
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATION_PERMISSION);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("需要通知权限才能使用该功能");
                builder.setPositiveButton("确定", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivityForResult(intent, REQUEST_CODE_NOTIFICATION_PERMISSION);
                });
                builder.setNegativeButton("取消", (dialog, which) -> {
                    Utils.toast("请授予通知权限", Toast.LENGTH_LONG);
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult requestCode:" + requestCode + " permissions:" + Arrays.toString(permissions) + " grantResults:" + Arrays.toString(grantResults));
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                Utils.toast("Notification permission was granted.");

                mPairImageView.setVisibility(View.VISIBLE);
            } else {
                Utils.toast("Notification permission request was denied.", Toast.LENGTH_LONG);
            }
        }
    }

    public void createNotification() {
        RemoteInput remoteInput = new RemoteInput.Builder("paring_code")
            .setLabel("请输入WLAN配对码")
            .build();
        Intent replayIntent = new Intent("input_paring_code")
            .setPackage(getPackageName());
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this,
            1,
            replayIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Action action = new Notification.Action.Builder(R.mipmap.ic_launcher, "请点击此处输入WLAN配对码", replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build();

        NotificationChannel channel = new NotificationChannel("secondaryscreen", "notifications", NotificationManager.IMPORTANCE_MAX);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
        Notification notification = new Notification.Builder(this, "secondaryscreen")
            .setAutoCancel(false)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("监测到WLAN-ADB调试配对服务已开启")
            .setContentText("请输入WLAN配对码，点击下方输入框即可输入⬇\uFE0F")
            .addAction(action)
            .build();

        notificationManager.notify(1, notification);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter("input_paring_code");
        filter.addCategory(getPackageName());
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = RemoteInput.getResultsFromIntent(intent);
                if (bundle != null) {
                    CharSequence pairingCode = bundle.getCharSequence("paring_code");
                    Log.i(TAG, "pairingCode:" + pairingCode);
                    if (pairingCode != null && pairingCode.length() == 6) {
                        adbPair(pairingCode.toString());
                    }
                }
            }
        }, filter);
    }

    private void adbPair(String pairingCode) {
        AdbShell.getInstance().pair(pairingCode, () -> {
            final boolean connected = AdbShell.getInstance().getConnectStatus();
            Log.i(TAG, "connected:" + connected);
            replyNotification(connected);

            Intent intent = new Intent(Utils.getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("connected", connected ? "OK" : "FAIL");
            startActivity(intent);
        });
    }

    private void replyNotification(boolean connected) {
        Notification.Builder replyBuilder = new Notification.Builder(MainActivity.this, "secondaryscreen");
        replyBuilder.setSmallIcon(R.mipmap.ic_launcher);
        replyBuilder.setAutoCancel(true);
        if (connected) {
            replyBuilder.setContentTitle("WLAN-ADB调试配对成功\uD83D\uDE0A");
            replyBuilder.setContentText("您现在可以使用WLAN-ADB调试功能了\uD83C\uDF89\uD83C\uDF89\uD83C\uDF89");
        } else {
            replyBuilder.setContentTitle("WLAN-ADB调试配对失败");
            replyBuilder.setContentText("请先关闭再开启无线调试功能后再次进行配对\uD83D\uDE0A");
        }

        Notification replyNotification = replyBuilder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, replyNotification);

        Utils.runOnOtherThread(() -> {
            Utils.sleep(1000);
            Utils.runOnUiThread(() -> {
                notificationManager.cancel(1);
            });
        });
    }

    private void setAdbConnectionVisibility(boolean visibility) {
        findViewById(R.id.adb_connection).setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    private boolean tryAdbTcpipConnect() {
        boolean tryConnect = false;
        String portString = Utils.getAdbTcpipPort();
        if (portString != null && !portString.isEmpty()) {
            tryConnect = true;
            AdbShell.getInstance().connect(Integer.parseInt(portString), () -> {
                if (AdbShell.getInstance().getConnectStatus()) {
                    Utils.toast("ADB连接成功");
                    setAdbConnectionVisibility(false);
                } else {
                    hidePairImageView();
                }
            });
        }
        return tryConnect;
    }

    private void adbTcpipConnect() {
        View inputView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);

        EditText input_text = inputView.findViewById(R.id.input_text);
        input_text.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        String portString = Utils.getAdbTcpipPort();
        if (portString != null && !portString.isEmpty()) {
            input_text.setText(portString);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(inputView);

        builder.setTitle("ADB-TCPIP调试");
        builder.setMessage("请开启ADB-TCPIP调试，在PC端执行：adb tcpip 5555");

        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("连接", (DialogInterface dialog, int which) -> {
            String portNumberString = input_text.getText().toString();

            if (portNumberString != null
                    && !portNumberString.isEmpty()
                    && TextUtils.isDigitsOnly(portNumberString)) {
                int port = Integer.parseInt(portNumberString);
                AdbShell.getInstance().connect(port, () -> {
                    if (AdbShell.getInstance().getConnectStatus()) {
                        Utils.toast("ADB连接成功");

                        setAdbConnectionVisibility(false);
                    } else {
                        Utils.toast("ADB连接失败，请输入正确的端口号");
                    }
                });
            } else {
                Utils.toast("请输入正确的端口号");
            }
        });
        builder.show();
    }

    private void adbPairConnect() {
        AdbShell.getInstance().getPairingPort(() -> {
            createNotification();
        });

        openDeveloperOptions();
    }

    private boolean checkVirtualDisplayReady() {
        boolean ready = Utils.checkVirtualDisplayReady();
        if (ready) {
            setAdbConnectionVisibility(false);
        } else {
            setAdbConnectionVisibility(true);

            if (tryAdbTcpipConnect()) {
                Utils.toast("正在尝试使用TCPIP方式连接ADB");
            } else {
                Utils.toast("请先连接ADB");
                hidePairImageView();
            }
        }

        return ready;
    }

    private void openDeveloperOptions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        startActivity(intent);
    }

    private void hidePairImageView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasNotificationPermission()) {
                registerReceiver();
            } else {
                mPairImageView.setVisibility(View.GONE);
                requestNotificationsPermission();
            }
        } else {
            mPairImageView.setVisibility(View.GONE);
        }
    }
}
