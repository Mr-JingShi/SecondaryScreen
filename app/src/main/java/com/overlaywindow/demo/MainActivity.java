package com.overlaywindow.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";

    private static int REQUEST_CODE = 1001;

    private static FloatWindow mFloatWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MainActivity onCreate");
        super.onCreate(savedInstanceState);

        if (mFloatWindow != null) {
            Toast.makeText(this, getString(R.string.make_happy), Toast.LENGTH_SHORT).show();

            mFloatWindow.show();

            finish();
        } else if (hasPermission()) {
            startFloatWindow();

            finish();
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (hasPermission()) {
                startFloatWindow();
            } else {
                Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }

    private void startFloatWindow() {
        Log.i(TAG, "MainActivity startFloatWindow");
        mFloatWindow = new FloatWindow();
        mFloatWindow.show();
    }

    private boolean hasPermission() {
        return Settings.canDrawOverlays(this);
    }
}
