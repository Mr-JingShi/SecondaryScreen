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
            Log.i(TAG, "SecondActivity onTouchEvent: " + event);
            float x = event.getRawX();
            float y = event.getRawY();
            float oldX = mTextView.getX();
            float oldY = mTextView.getY();

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (x > oldX && x < oldX + mTextView.getWidth()
                        && y > oldY && y < oldY + mTextView.getHeight()) {
                    isMatch = true;
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (isMatch) {
                    x = x - mTextView.getWidth() / 2;
                    y = y - mTextView.getHeight() / 2;

                    x = Math.max(0, Math.min(x, getResources().getDisplayMetrics().widthPixels - mTextView.getWidth()));
                    y = Math.max(0, Math.min(y, getResources().getDisplayMetrics().heightPixels - mTextView.getHeight()));

                    mTextView.setX(x);
                    mTextView.setY(y);
                }
            } else {
                isMatch = false;
            }
            return true;
        });
    }
}
