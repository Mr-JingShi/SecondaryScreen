package com.overlaywindow.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {
    private static String TAG = "SecondActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "SecondActivity onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second);

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
}
