package com.secondaryscreen.app;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {
    private static String TAG = "SecondActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setTaskDescription(new ActivityManager.TaskDescription("副屏桌面"));

        Display display = getDisplay();
        Log.d(TAG, "display.getDisplayId():" + display.getDisplayId());
        if (0 == display.getDisplayId()) {
            Utils.toast("副屏桌面不允许运行在主屏上");

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
        setContentView(R.layout.activity_second);

        View view = findViewById(R.id.second_image);
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        Drawable drawable = wallpaperManager.getBuiltInDrawable();
        view.setBackground(drawable);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }
}
