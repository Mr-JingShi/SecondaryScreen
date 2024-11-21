package com.secondaryscreen.app;

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

    private View mParentWindowContent;

    public FloatIcon(View windowContent) {
        mParentWindowContent = windowContent;

        mShowImageView = new ImageView(Utils.getContext());
        mShowImageView.setImageResource(R.drawable.go_left);
        mShowImageView.setOnClickListener((View v) -> {
            mParentWindowContent.setVisibility(View.VISIBLE);
            mShowImageView.setVisibility(View.GONE);
        });
        mShowImageView.setVisibility(View.GONE);

        WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        windowParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.alpha = 1.0f;
        windowParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        WindowManager wm = (WindowManager)Utils.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mShowImageView, windowParams);
    }

    public void show() {
        mShowImageView.setVisibility(View.VISIBLE);
        mParentWindowContent.setVisibility(View.GONE);
    }
}