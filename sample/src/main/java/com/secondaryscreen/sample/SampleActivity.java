package com.secondaryscreen.sample;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class SampleActivity extends AppCompatActivity {
    private static String TAG = "SampleActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "SampleActivity onCreate");
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_sample);

        findViewById(R.id.title).setOnTouchListener((view, event) -> {
            Log.i(TAG, "SampleActivity onTouchEvent: " + event);
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX() - view.getWidth() / 2;
                float y = event.getRawY() - view.getHeight() / 2;

                x = Math.max(0, Math.min(x, getResources().getDisplayMetrics().widthPixels - view.getWidth()));
                y = Math.max(0, Math.min(y, getResources().getDisplayMetrics().heightPixels - view.getHeight()));

                view.setX(x);
                view.setY(y);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "SampleActivity onResume");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            loadSecondActivity();
        }).start();
    }

    private void loadSecondActivity() {
        DisplayManager displayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        Log.i(TAG, "loadSecondActivity displays.length:" + displays.length);
        if (displays.length > 1) {
            Intent intent = new Intent(this, SecondActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            boolean isAllowed = am.isActivityStartAllowedOnDisplay(this, displays[1].getDisplayId(), intent);
            Log.i(TAG, "isAllowed:" + isAllowed);
            if (isAllowed) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(displays[1].getDisplayId());
                try {
                    startActivity(intent, options.toBundle());
                } catch (Exception e) {
                    Log.e(TAG, "loadSecondActivity", e);
                }
            }
        }
    }
}
