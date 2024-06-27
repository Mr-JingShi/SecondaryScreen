package com.overlaywindow.sample;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SampleActivity extends AppCompatActivity {
    private static String TAG = "SampleActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "SampleActivity onCreate");
        super.onCreate(savedInstanceState);

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
        if (displays.length > 1) {
            for (Display display :displays){
                if (display.getName().contains("PC")) {
                    return;
                }
            }
            Intent intent = new Intent(this, SecondActivity.class);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displays[1].getDisplayId());
            startActivity(intent, options.toBundle());
        }
    }

}
