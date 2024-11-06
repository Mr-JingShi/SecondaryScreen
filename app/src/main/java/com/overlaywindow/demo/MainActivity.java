package com.overlaywindow.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";

    private static int REQUEST_CODE = 1001;

    private FloatWindow mFloatWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MainActivity onCreate");
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        RadioButton radio_single = findViewById(R.id.radio_single);
        RadioButton radio_master = findViewById(R.id.radio_master);
        RadioButton radio_slave = findViewById(R.id.radio_slave);
        TextView wlan_description = findViewById(R.id.wlan_description);
        TextView wlan_address = findViewById(R.id.wlan_address);
        String remoteHost = PrivatePreferences.getString("remoteHost", "");
        wlan_address.setText(remoteHost);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            radio_single.setVisibility(View.GONE);
            radio_master.setVisibility(View.GONE);
            radio_slave.setChecked(true);
            wlan_description.setVisibility(View.VISIBLE);
            wlan_address.setVisibility(View.VISIBLE);
        } else {
            radio_single.setOnClickListener((view) -> {
                wlan_description.setVisibility(View.GONE);
                wlan_address.setVisibility(View.GONE);
            });
            radio_master.setOnClickListener((view) -> {
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
                wlan_description.setVisibility(View.VISIBLE);
                wlan_description.setText("主设备WLAN地址：");
                wlan_address.setVisibility(View.VISIBLE);
            });
        }

        findViewById(R.id.start_button).setOnClickListener((view) -> {
            if (radio_single.isChecked()) {
                Utils.setIsSingleMachineMode(true);

                startFloatWindow();
            } else if (radio_master.isChecked()) {
                Utils.setIsSingleMachineMode(false);

                if (!Utils.checkSelfWifi()) {
                    Utils.toast("请连接WIFI");
                    return;
                }

                if (Utils.checkVirtualDisplayReady()) {
                    Utils.toast("主设备已准备就绪。");
                    finish();
                } else {
                    startFloatWindow();
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
                    PrivatePreferences.putString("remoteHost", wlanAddress);

                    Utils.hideKeyboard(view);

                    Utils.setRemoteHost(wlanAddress);

                    finish();

                    Intent intent = new Intent(this, SecondaryScreenActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (hasPermission()) {
                showFloatWindow();
            } else {
                Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }

    private void startFloatWindow() {
        if (hasPermission()) {
            showFloatWindow();

            finish();
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    private void showFloatWindow() {
        Log.i(TAG, "showFloatWindow");
        mFloatWindow = new FloatWindow();
        mFloatWindow.show();
    }

    private boolean hasPermission() {
        return Settings.canDrawOverlays(this);
    }
}
