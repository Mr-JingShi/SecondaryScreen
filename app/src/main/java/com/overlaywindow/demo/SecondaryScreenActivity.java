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

import androidx.appcompat.app.AppCompatActivity;

public class SecondaryScreenActivity extends AppCompatActivity {
    private static String TAG = "SecondaryScreenSlaveActivity";
    private TextureView mTextureView;
    private ControlClient mControlClient;
    private VideoClient mVideoClient;
    private DisplayClient mVirtualDisplayClient;
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
        mVirtualDisplayClient = new DisplayClient();

        Intent intent = getIntent();
        if (intent != null) {
            String remoteHost = intent.getStringExtra("remoteHost");

            Log.i(TAG, "remoteHost:" + remoteHost);
            if (remoteHost != null && !remoteHost.isEmpty()) {
                mVideoClient.setRemoteHost(remoteHost);
                mControlClient.setRemoteHost(remoteHost);
                mVirtualDisplayClient.setRemoteHost(remoteHost);
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

        int rotation = defaultDisplay.getRotation();

        mTextureView = findViewById(R.id.secondaryscreen_texture);
        mTextureView.setPivotX(0);
        mTextureView.setPivotY(0);
        mTextureView.getLayoutParams().width = displayMetrics.widthPixels;
        mTextureView.getLayoutParams().height = displayMetrics.heightPixels;
        mTextureView.setOpaque(false);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        Log.i(TAG, "displayMetrics widthPixels:" + displayMetrics.widthPixels + " heightPixels:" + displayMetrics.heightPixels);

        if (rotation % 2 == 1) {
            mVirtualDisplayClient.setScreenInfo(1, displayMetrics.heightPixels, displayMetrics.widthPixels, rotation, displayMetrics.densityDpi);
        } else {
            mVirtualDisplayClient.setScreenInfo(1, displayMetrics.widthPixels, displayMetrics.heightPixels, rotation, displayMetrics.densityDpi);
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    Log.i(TAG, "onSurfaceTextureAvailable width:" + width + " height:" + height);
                    mVirtualDisplayClient.start();

                    Utils.runOnOtherThread(() -> {
                        Utils.sleep(1000);
                        mVideoClient.start(new Surface(surfaceTexture));
                        mControlClient.start();

                        Utils.sleep(1000);

                        runOnUiThread(() -> {
                            if (mControlClient.getConnected()) {
                                mTextureView.setOnTouchListener((view, event) -> {
                                    Utils.offerEvent(event);
                                    return true;
                                });
                            } else {
                                Utils.toast("connect failed");

                                SecondaryScreenActivity.this.finish();

                                Intent intent = new Intent(SecondaryScreenActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        });
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
                    Log.i(TAG, "onDisplayChanged:" + displayId);
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
                            Log.i(TAG, "displayMetrics widthPixels:" + displayMetrics.widthPixels + " heightPixels:" + displayMetrics.heightPixels);

                            if (rotation % 2 == 1) {
                                mVirtualDisplayClient.setScreenInfo(1, displayMetrics.heightPixels, displayMetrics.widthPixels, rotation, displayMetrics.densityDpi);
                            } else {
                                mVirtualDisplayClient.setScreenInfo(1, displayMetrics.widthPixels, displayMetrics.heightPixels, rotation, displayMetrics.densityDpi);
                            }

                            mVirtualDisplayClient.start();
                        }
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            };
}
