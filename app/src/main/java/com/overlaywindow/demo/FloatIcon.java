package com.overlaywindow.demo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

// 部分逻辑参考自：
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/display/OverlayDisplayWindow.java

final class FloatIcon {
    private static final String TAG = "FloatIcon";

    private ImageView mShowImageView;

    private View mWindowContent;

    public FloatIcon(Context context, View windowContent) {
        mWindowContent = windowContent;

        mShowImageView = new ImageView(context);
        mShowImageView.setImageResource(R.drawable.go_left);
        mShowImageView.setOnClickListener(mShowListener);
        mShowImageView.setVisibility(View.GONE);

        WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        windowParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.alpha = 0.8f;
        windowParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        windowParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        windowParams.width = 48;
        windowParams.height = 48;

        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(mShowImageView, windowParams);
    }

    public void show() {
        mShowImageView.setVisibility(View.VISIBLE);
    }
    public void hide() {
        mShowImageView.setVisibility(View.GONE);
    }

    private final View.OnClickListener mShowListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mShowImageView.setVisibility(View.GONE);
                    mWindowContent.setVisibility(View.VISIBLE);
                }
            };
}