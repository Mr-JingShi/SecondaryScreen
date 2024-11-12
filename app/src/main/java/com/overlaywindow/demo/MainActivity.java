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
        boolean needShowSpinner = chooseResolution(spinner);
        if (!needShowSpinner) {
            spinner.setVisibility(View.GONE);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            radio_single.setVisibility(View.GONE);
            radio_master.setVisibility(View.GONE);
            radio_slave.setChecked(true);
            wlan_description.setVisibility(View.VISIBLE);
            wlan_address.setVisibility(View.VISIBLE);
        } else {
            radio_single.setOnClickListener((view) -> {
                if (needShowSpinner) {
                    spinner.setVisibility(View.VISIBLE);
                }
                wlan_description.setVisibility(View.GONE);
                wlan_address.setVisibility(View.GONE);
            });
            radio_master.setOnClickListener((view) -> {
                if (needShowSpinner) {
                    spinner.setVisibility(View.GONE);
                }
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
                if (needShowSpinner) {
                    spinner.setVisibility(View.VISIBLE);
                }
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

    private boolean chooseResolution(Spinner spinner) {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        if (display.getRotation() % 2 == 0) {
            return chooseResolution(spinner, realMetrics.widthPixels, realMetrics.heightPixels, realMetrics.densityDpi, metrics.widthPixels, metrics.heightPixels);
        }
        return chooseResolution(spinner, realMetrics.heightPixels, realMetrics.widthPixels, realMetrics.densityDpi, metrics.heightPixels, metrics.widthPixels);
    }

    private boolean chooseResolution(Spinner spinner, int virtualDisplayWidth, int virtualDisplayHeight, int virutalDisplayDensityDpi, int textureViewWidth, int textureViewHeight) {
        Log.d(TAG, "chooseResolution virtualDisplayWidth:" + virtualDisplayWidth + " virtualDisplayHeight:" + virtualDisplayHeight + " virutalDisplayDensityDpi:" + virutalDisplayDensityDpi + " textureViewWidth:" + textureViewWidth + " textureViewHeight:" + textureViewHeight);

        ArrayList<Resolution> resolutions = new ArrayList<>();
        resolutions.add(new Resolution(480, 720, 142));
        resolutions.add(new Resolution(720, 1280, 213));
        resolutions.add(new Resolution(1080, 1920, 320));
        resolutions.add(new Resolution(2160, 3840, 320));

        ArrayList<String> resolutionStrings = new ArrayList<>();
        for (int i = resolutions.size() - 1 ; i >= 0; i--) {
            int width = resolutions.get(i).TEXTUREVIEW_WIDTH;
            int height = resolutions.get(i).TEXTUREVIEW_HEIGHT;
            if (textureViewWidth >= width * 2 && textureViewHeight >= height * 2) {
                resolutions.get(i).TEXTUREVIEW_WIDTH *= 2;
                resolutions.get(i).TEXTUREVIEW_HEIGHT *= 2;
            } else if (textureViewWidth >= width * 1.5 && textureViewHeight >= height * 1.5) {
                resolutions.get(i).TEXTUREVIEW_WIDTH *= 1.5;
                resolutions.get(i).TEXTUREVIEW_HEIGHT *= 1.5;
            } else if (textureViewWidth >= width && textureViewHeight >= height) {
                // do nothing
            } else if (textureViewWidth >= width * 0.5 && textureViewHeight >= height * 0.5) {
                resolutions.get(i).TEXTUREVIEW_WIDTH *= 0.5;
                resolutions.get(i).TEXTUREVIEW_HEIGHT *= 0.5;
            } else {
                resolutions.remove(i);
                continue;
            }

            Log.d(TAG, "chooseResolution RESOLUTIONS[" + i + "]:" + resolutions.get(i).toString());
            String tag = "";
            if (resolutions.get(i).VIRTUALDISPLAY_WIDTH == 480) {
                tag = "480p";
            } else if (resolutions.get(i).VIRTUALDISPLAY_WIDTH == 720) {
                tag = "720p";
            } else if (resolutions.get(i).VIRTUALDISPLAY_WIDTH == 1080) {
                tag = "1080p";
            } else if (resolutions.get(i).VIRTUALDISPLAY_WIDTH == 2160) {
                tag = "4K";
            }
            resolutionStrings.add(tag
                    + "("
                    + resolutions.get(i).VIRTUALDISPLAY_WIDTH
                    + "x"
                    + resolutions.get(i).VIRTUALDISPLAY_HEIGHT
                    + "/"
                    + resolutions.get(i).VIRTUALDISPLAY_DENSITYDPI
                    + ")");
        }


        if (!resolutions.isEmpty()) {
            Collections.reverse(resolutionStrings);
            resolutions.add(new Resolution(virtualDisplayWidth,
                    virtualDisplayHeight,
                    virutalDisplayDensityDpi,
                    textureViewWidth,
                    textureViewHeight));
            resolutionStrings.add("slef"
                    + "("
                    + virtualDisplayWidth
                    + "x"
                    + virtualDisplayHeight
                    + "/"
                    + virutalDisplayDensityDpi
                    + ")");

            spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, resolutionStrings));

            String resolution = PrivatePreferences.getString("resolution", "");
            Log.i(TAG, "resolution:" + resolution);
            if (!resolution.isEmpty()) {
                for (int i = 0; i < resolutionStrings.size(); i++) {
                    if (resolutionStrings.get(i).equals(resolution)) {
                        spinner.setSelection(i, true);

                        Resolution.R = new Resolution(resolutions.get(i).TEXTUREVIEW_WIDTH,
                                resolutions.get(i).TEXTUREVIEW_HEIGHT,
                                resolutions.get(i).VIRTUALDISPLAY_DENSITYDPI,
                                resolutions.get(i).TEXTUREVIEW_WIDTH,
                                resolutions.get(i).TEXTUREVIEW_HEIGHT);
                        break;
                    }
                }
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String item = (String) adapterView.getItemAtPosition(i);
                    Utils.toast(item);

                    Log.i(TAG, "i:" + i + " item:" + item + " resolutions:" + resolutions.get(i).toString());

                    Resolution.R = new Resolution(resolutions.get(i).TEXTUREVIEW_WIDTH,
                            resolutions.get(i).TEXTUREVIEW_HEIGHT,
                            resolutions.get(i).VIRTUALDISPLAY_DENSITYDPI,
                            resolutions.get(i).TEXTUREVIEW_WIDTH,
                            resolutions.get(i).TEXTUREVIEW_HEIGHT);

                    PrivatePreferences.putString("resolution", item);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            return true;
        }

        Resolution.R = new Resolution(virtualDisplayWidth, virtualDisplayHeight, virutalDisplayDensityDpi, textureViewWidth, textureViewHeight);
        return false;
    }
}
