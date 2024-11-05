package com.overlaywindow.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondaryScreenActivity extends AppCompatActivity {
    private static String TAG = "SecondaryScreenSlaveActivity";
    private TextureView mTextureView;
    private ControlClient mControlClient;
    private VideoClient mVideoClient;
    private DisplayClient mDisplayClient;
    private int mRotation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "SecondaryScreenSlaveActivity onCreate");
        super.onCreate(savedInstanceState);

        Utils.setIsSingleMachineMode(false);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(R.layout.activity_secondaryscreen);

        mVideoClient = new VideoClient();
        mControlClient = new ControlClient();
        mDisplayClient = new DisplayClient();

        Intent intent = getIntent();
        if (intent != null) {
            String remoteHost = intent.getStringExtra("remoteHost");

            Log.i(TAG, "remoteHost:" + remoteHost);
            if (remoteHost != null && !remoteHost.isEmpty()) {
                mControlClient.setRemoteHost(remoteHost);
                mDisplayClient.setRemoteHost(remoteHost);
            } else {
                throw new RuntimeException("remoteHost error");
            }
        }

        DisplayManager dm = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        dm.registerDisplayListener(mDisplayListener, null);

        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = wm.getDefaultDisplay();
        mRotation = defaultDisplay.getRotation();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);

        TextView textView = findViewById(R.id.secondaryscreen_title);
        textView.bringToFront();

        mTextureView = findViewById(R.id.secondaryscreen_texture);
        mTextureView.setPivotX(0);
        mTextureView.setPivotY(0);
        mTextureView.getLayoutParams().width = displayMetrics.widthPixels;
        mTextureView.getLayoutParams().height = displayMetrics.heightPixels;
        mTextureView.setOpaque(false);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mTextureView.requestLayout();
        Log.i(TAG, "displayMetrics widthPixels:" + displayMetrics.widthPixels + " heightPixels:" + displayMetrics.heightPixels);

        if (mRotation % 2 == 1) {
            mDisplayClient.setScreenInfo(1, displayMetrics.heightPixels, displayMetrics.widthPixels, mRotation, displayMetrics.densityDpi);
        } else {
            mDisplayClient.setScreenInfo(1, displayMetrics.widthPixels, displayMetrics.heightPixels, mRotation, displayMetrics.densityDpi);
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mDisplayClient.shutdown();
        mVideoClient.shutdown();
        mControlClient.shutdown();
        DisplayManager dm = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        dm.unregisterDisplayListener(mDisplayListener);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    Log.i(TAG, "onSurfaceTextureAvailable width:" + width + " height:" + height);

                    mVideoClient.start(new Surface(surfaceTexture));
                    mDisplayClient.start();
                    mControlClient.start();

                    mTextureView.setOnTouchListener((view, event) -> {
                        Utils.offerMotionEvent(event);
                        return true;
                    });
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    // Log.v(TAG, "onSurfaceTextureDestroyed");
                    return true;
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                    // Log.i(TAG, "onSurfaceTextureSizeChanged width:" + width + " height:" + height);
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                    // Log.v(TAG, "onSurfaceTextureUpdated");
                }
            };

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId == 0) {
                        int rotation = Utils.getRotation();
                        if (mRotation != rotation) {
                            mRotation = rotation;

                            WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
                            Display defaultDisplay = wm.getDefaultDisplay();
                            DisplayMetrics displayMetrics = new DisplayMetrics();
                            defaultDisplay.getRealMetrics(displayMetrics);

                            mTextureView.getLayoutParams().width = displayMetrics.widthPixels;
                            mTextureView.getLayoutParams().height = displayMetrics.heightPixels;
                            mTextureView.requestLayout();

                            Log.i(TAG, "displayMetrics widthPixels:" + displayMetrics.widthPixels + " heightPixels:" + displayMetrics.heightPixels);

                            if (rotation % 2 == 1) {
                                mDisplayClient.setScreenInfo(1, displayMetrics.heightPixels, displayMetrics.widthPixels, rotation, displayMetrics.densityDpi);
                            } else {
                                mDisplayClient.setScreenInfo(1, displayMetrics.widthPixels, displayMetrics.heightPixels, rotation, displayMetrics.densityDpi);
                            }
                        }
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            };
}
