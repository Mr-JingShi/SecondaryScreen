package com.overlaywindow.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private static int STANDARD_WIDTH = 1080;
    private static int STANDARD_HEIGHT = 1920;
    private static int STANDARD_DENSITYDPI = 320;
    private static int REQUEST_CODE = 1001;
    private static FloatWindow mFloatWindow;

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

        Spinner spinner = findViewById(R.id.spinner);

        RadioButton radio_single = findViewById(R.id.radio_single);
        RadioButton radio_master = findViewById(R.id.radio_master);
        RadioButton radio_slave = findViewById(R.id.radio_slave);
        TextView wlan_description = findViewById(R.id.wlan_description);
        TextView wlan_address = findViewById(R.id.wlan_address);
        String remoteHost = PrivatePreferences.getString("remoteHost", "");
        wlan_address.setText(remoteHost);
        chooseResolution(spinner);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            radio_single.setVisibility(View.GONE);
            radio_master.setVisibility(View.GONE);
            radio_slave.setChecked(true);
            wlan_description.setVisibility(View.VISIBLE);
            wlan_address.setVisibility(View.VISIBLE);
        } else {
            radio_single.setOnClickListener((view) -> {
                spinner.setVisibility(View.VISIBLE);
                wlan_description.setVisibility(View.GONE);
                wlan_address.setVisibility(View.GONE);
            });
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

    private void chooseResolution(Spinner spinner) {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        if (display.getRotation() % 2 == 0) {
            Resolution selfResolution = new Resolution("self", realMetrics.widthPixels, realMetrics.heightPixels, realMetrics.densityDpi, metrics.widthPixels, metrics.heightPixels);
            chooseResolution(spinner, selfResolution);
        } else {
            Resolution selfResolution = new Resolution("self", realMetrics.heightPixels, realMetrics.widthPixels, realMetrics.densityDpi, metrics.heightPixels, metrics.widthPixels);
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

        String resolution = PrivatePreferences.getString("resolution", "");
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

                PrivatePreferences.putString("resolution", item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }
}
