package com.secondaryscreen.app;

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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static String TAG = "MainActivity";
    private static int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private static int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;
    private static List<FloatWindow> mFloatWindow;
    private List<ApplicationInfo> mApplications;
    private List<Integer> mSelectPositions;
    private AppAdapter mAppAdapter;
    private DrawerLayout mDrawerLayout;
    private View mAppListContainer;
    private ImageView mTcpipImageView;
    private ImageView mPairImageView;
    private TextView mSlectAppTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MainActivity onCreate");
        super.onCreate(savedInstanceState);

        if (mFloatWindow != null) {
            Utils.toast("检测到悬浮窗在运行，请先关闭悬浮窗！");
            finish();
            return;
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

        processPowerSavePermission();

        mTcpipImageView = findViewById(R.id.adb_tcpip);
        mTcpipImageView.setOnClickListener((View v) -> {
            adbTcpipConnect();
        });

        mPairImageView = findViewById(R.id.adb_pair);
        mPairImageView.setOnClickListener((View v) -> {
            adbPairConnect();
        });

        chooseResolution();

        findViewById(R.id.start_button).setOnClickListener((view) -> {
            if (checkJarReady()) {
                startFloatWindow();
            }
        });

        mApplications = Utils.loadApplicationList();
        mSelectPositions = new ArrayList<>();
        String startAppList = PrivatePreferences.getStartAppList();
        if (startAppList != null && !startAppList.isEmpty()) {
            List<String> appList = Arrays.asList(startAppList.split(","));
            for (String app : appList) {
                Log.i(TAG, "app:" + app);
                for (int i = 0; i < mApplications.size(); i++) {
                    ApplicationInfo appInfo = mApplications.get(i);
                    if (app.equals(appInfo.packageName)) {
                        mSelectPositions.add(i);

                        String activityName = Utils.resolveTargetApp(appInfo.packageName);
                        if (activityName != null) {
                            activityName = appInfo.packageName + "/" + activityName;
                            Utils.addSelectActivityName(activityName);
                        }
                    }
                }
            }
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                Log.i(TAG, "onDrawerClosed");
                String startAppList = "";
                for (Integer position : mSelectPositions) {
                    ApplicationInfo app = (ApplicationInfo) mAppAdapter.getItem(position);
                    Log.i(TAG, "onDrawerClosed position:" + position + " app:" + app);

                    String activityName = Utils.resolveTargetApp(app.packageName);
                    if (activityName != null) {
                        activityName = app.packageName + "/" + activityName;
                        Utils.addSelectActivityName(activityName);
                    }

                    startAppList += app.packageName + ",";
                }
                if (!startAppList.isEmpty()) {
                    startAppList = startAppList.substring(0, startAppList.length() - 1);
                    PrivatePreferences.setStartAppList(startAppList);
                }

                int size = mSelectPositions.size();
                mSlectAppTextView.setText(size == 0 ? "请选择应用" : "已选择" + size + "个应用");
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        mAppListContainer = findViewById(R.id.app_list_container);

        ListView appListView = findViewById(R.id.app_list);
        appListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Log.i(TAG, "setOnItemClickListener position:" + position);

            // mDrawerLayout.closeDrawer(mAppListContainer);

            Integer positionInt = Integer.valueOf(position);
            if (mSelectPositions.contains(positionInt)) {
                mSelectPositions.remove(positionInt);
            } else {
                int size = mSelectPositions.size();
                if (size >= 5) {
                    mSelectPositions.remove(0);
                }
                mSelectPositions.add(positionInt);
            }
            mAppAdapter.notifyDataSetChanged();
        });

        mAppAdapter = new AppAdapter();
        appListView.setAdapter(mAppAdapter);

        mSlectAppTextView = findViewById(R.id.select_app);
        int size = mSelectPositions.size();
        mSlectAppTextView.setText(size == 0 ? "请选择应用" : "已选择" + size + "个应用");
        mSlectAppTextView.setOnClickListener((view) -> {
            mDrawerLayout.openDrawer(mAppListContainer);
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
                Log.i(TAG, "WLAN-ADB调试配对结果:" + connected);
                Utils.toast("WLAN-ADB调试配对结果:" + connected);

                if (connected.equals("OK")) {
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

        Log.i(TAG, "MainActivity onDestroy");
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
        mFloatWindow = new ArrayList<>();
        ControlConnection.getInstance().start();
        DisplayConnection.getInstance().start();
        for (int i = 0; i < 5; i++) {
            FloatWindow floatWindow = new FloatWindow(i);
            mFloatWindow.add(floatWindow);
            floatWindow.show();
        }
    }

    public static void clearFloatWindow() {
        Utils.finishAndRemoveTask(MainActivity.class.getName());
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private void chooseResolution() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        Resolution.R = new Resolution("self",
                realMetrics.widthPixels,
                realMetrics.heightPixels,
                realMetrics.densityDpi,
                metrics.widthPixels,
                metrics.heightPixels);
    }

    public boolean hasNotificationPermission() {
        // https://developer.android.com/develop/ui/views/notifications/notification-permission?hl=zh-cn
        // Android 13（API 级别 33）及更高版本支持用于从应用发送非豁免（包括前台服务 [FGS]）通知的运行时权限：POST_NOTIFICATIONS。此更改有助于用户专注于最重要的通知。
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
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
            .setContentText("请输入WLAN配对码，点击下方即可输入⬇\uFE0F")
            .addAction(action)
            .build();

        notificationManager.notify(1, notification);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter("input_paring_code");
        filter.addCategory(getPackageName());
        /**
         * java.lang.SecurityException: com.secondaryscreen.app: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts
         * As discussed at Google I/O 2023, registering receivers with intention using the RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED flag was introduced as part of Android 13 and is now a requirement for apps running on Android 14 or higher (U+).
         * If you do not implement this, the system will throw a security exception.
         * To allow the broadcast receiver to receive broadcasts from other apps, register the receiver using the following code:
         * context.registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED);
         * To register a broadcast receiver that does not receive broadcasts from other apps, including system apps, register the receiver using the following code:
         * context.registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED);
         */
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
        }, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void adbPair(String pairingCode) {
        AdbShell.getInstance().pair(pairingCode, () -> {
            final boolean connected = AdbShell.getInstance().getConnectStatus();
            Log.i(TAG, "connected:" + connected);
            if (connected) {
                PrivatePreferences.setServiceAdbTslPort(AdbShell.getInstance().getTlsPort());
            }
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

    private void tryAdbConnect() {
        Runnable runnable = () -> {
            boolean connected = AdbShell.getInstance().getConnectStatus();
            Log.i(TAG, "runnable connected:" + connected);
            if (connected) {
                PrivatePreferences.setServiceAdbTslPort(AdbShell.getInstance().getTlsPort());

                Utils.toast("ADB连接成功");
                setAdbConnectionVisibility(false);
            } else {
                Utils.toast("ADB连接失败");
                if (supportWlanDebug()) {
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
        };

        Runnable runnable1 = () -> {
            boolean connected = AdbShell.getInstance().getConnectStatus();
            Log.i(TAG, "runnable1 connected:" + connected);
            if (connected) {
                Utils.toast("ADB连接成功");
                setAdbConnectionVisibility(false);
            } else {
                PrivatePreferences.setServiceAdbTslPort(0);

                Log.i(TAG, "runnable1 autoConnect");
                AdbShell.getInstance().autoConnect(runnable);
            }
        };

        Runnable runnable2 = () -> {
            int port = PrivatePreferences.getServiceAdbTslPort();
            if (port != 0) {
                Log.i(TAG, "ServiceAdbTslPort:" + port);
                AdbShell.getInstance().connect(port, runnable1);
            } else {
                Log.i(TAG, "runnable2 autoConnect");
                AdbShell.getInstance().autoConnect(runnable);
            }
        };

        Runnable runnable3 = () -> {
            boolean connected = AdbShell.getInstance().getConnectStatus();
            Log.i(TAG, "runnable3 connected:" + connected);
            if (connected) {
                Utils.toast("ADB连接成功");
                setAdbConnectionVisibility(false);
            } else {
                if (supportWlanDebug()) {
                    runnable2.run();
                } else {
                    mPairImageView.setVisibility(View.GONE);
                }
            }
        };

        Runnable runnable4 = () -> {
            String port = Utils.getServiceAdbTcpPort();
            if (port != null && !port.isEmpty()) {
                Log.i(TAG, "ServiceAdbTcpPort:" + port);
                AdbShell.getInstance().connect(Integer.parseInt(port), runnable3);
            } else {
                if (supportWlanDebug()) {
                    runnable2.run();
                } else {
                    mPairImageView.setVisibility(View.GONE);
                }
            }
        };

        Utils.toast("正在尝试ADB连接...");
        runnable4.run();
    }

    private void adbTcpipConnect() {
        View inputView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);

        EditText input_text = inputView.findViewById(R.id.input_text);
        input_text.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        String portString = Utils.getServiceAdbTcpPort();
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

    private boolean checkJarReady() {
        boolean ready = Utils.checkJarReady();
        if (ready) {
            setAdbConnectionVisibility(false);
        } else {
            setAdbConnectionVisibility(true);

            tryAdbConnect();
        }

        return ready;
    }

    private void openDeveloperOptions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        startActivity(intent);
    }

    private boolean supportWlanDebug() {
        // Andrid 11+ 支持无线调试
        // 华为设备禁用了无线调试功能
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Build.MANUFACTURER.equals("HUAWEI");
    }

    /**
     * 处理省电优化权限
     * @return
     */
    private boolean processPowerSavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("低电量优化策略");
                builder.setMessage("请允许副屏忽略低电量优化策略，以确保副屏能在低电量时正常运行");

                builder.setNegativeButton("取消", null);
                builder.setPositiveButton("开启", (DialogInterface dialog, int which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                });
                builder.show();
            }
        }
        return true;
    }

    private class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mApplications.size();
        }

        @Override
        public Object getItem(int position) {
            return mApplications.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_app_list, parent, false);
                holder = new ViewHolder();
                holder.button = convertView.findViewById(R.id.radio_button);
                holder.icon = convertView.findViewById(R.id.app_icon);
                holder.name = convertView.findViewById(R.id.app_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApplicationInfo info = (ApplicationInfo) getItem(position);
            holder.button.setChecked(mSelectPositions.contains(Integer.valueOf(position)));
            holder.icon.setBackground(info.loadIcon(getPackageManager()));
            holder.name.setText(info.loadLabel(getPackageManager()));

            return convertView;
        }

        class ViewHolder {
            RadioButton button;
            ImageView icon;
            TextView name;
        }
    }

}
