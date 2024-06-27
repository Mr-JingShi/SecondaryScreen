package com.overlaywindow.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {
    private static String TAG = "SecondActivity";
    private TextView mTextView;
    private boolean isMatch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "SecondActivity onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second);

        mTextView = findViewById(R.id.title);

        mTextView.setOnTouchListener((view, event) -> {
            Log.i(TAG, "SampleActivity onTouchEvent: " + event);
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX() - mTextView.getWidth() / 2;
                float y = event.getRawY() - mTextView.getHeight() / 2;

                x = Math.max(0, Math.min(x, getResources().getDisplayMetrics().widthPixels - mTextView.getWidth()));
                y = Math.max(0, Math.min(y, getResources().getDisplayMetrics().heightPixels - mTextView.getHeight()));

                mTextView.setX(x);
                mTextView.setY(y);
            }
            return true;
        });
    }
}
