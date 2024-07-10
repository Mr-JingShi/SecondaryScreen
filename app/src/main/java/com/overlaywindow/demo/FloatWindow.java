package com.overlaywindow.demo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

// 部分逻辑参考自：
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/display/OverlayDisplayWindow.java

final class FloatWindow {
    private static final String TAG = "FloatWindow";
    private final float INITIAL_SCALE = 0.5f;
    private final float MIN_SCALE = 0.3f;
    private final float MAX_SCALE = 1.0f;
    private final float WINDOW_ALPHA = 0.95f;
    private final boolean DISABLE_MOVE_AND_RESIZE = false;
    private final boolean ADD_FLAG_SECURE = false;
    private final boolean USE_SELF_VIRTUALDISPLAY = false;
    private final boolean USE_SURFACE_EVENT = false;
    private final int mDisplayId;
    private int mWidth;
    private int mHeight;
    private int mDensityDpi;
    private DisplayManager mDisplayManager;
    private WindowManager mWindowManager;
    private View mWindowContent;
    private ImageView mTcpipImageView;
    private ImageView mPairImageView;
    private TextView  mRemarkTextView;
    private ImageView mHideImageView;
    private ImageView mLockImageView;
    private ImageView mFocusImageView;
    private WindowManager.LayoutParams mWindowParams;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private boolean mWindowVisible;
    private int mWindowX;
    private int mWindowY;
    private float mWindowScale;
    private float mLiveTranslationX;
    private float mLiveTranslationY;
    private float mLiveScale = 1.0f;
    private float mRealScale;
    private FloatIcon mFloatIcon;
    private AdbDebug mAdbDebug;
    private FloatDialog mFloatDialog;
    private boolean isLocked = false;
    private boolean isFocused = false;
    private ControlClient mControlClient;
    private VideoClient mVideoClient;

    public FloatWindow() {
        mDisplayManager = (DisplayManager)DemoApplication.getApp().getSystemService(
                Context.DISPLAY_SERVICE);
        mWindowManager = (WindowManager)DemoApplication.getApp().getSystemService(
                Context.WINDOW_SERVICE);

        Display defaultDisplay = mWindowManager.getDefaultDisplay();

        mDisplayId = defaultDisplay.getDisplayId();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);
        
        resize(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, false);

        createWindow();

        mFloatIcon = new FloatIcon(mWindowContent);

        mAdbDebug = new AdbDebug();

        mFloatDialog = new FloatDialog(this, mAdbDebug);
    }

    public void show() {
        if (!mWindowVisible) {
            mDisplayManager.registerDisplayListener(mDisplayListener, null);

            clearLiveState();
            updateWindowParams();
            mWindowManager.addView(mWindowContent, mWindowParams);
            mWindowVisible = true;
        }
    }

    public void dismiss() {
        if (mWindowVisible) {
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
            mWindowManager.removeView(mWindowContent);
            mWindowVisible = false;
        }
    }

    public void resize(int width, int height, int densityDpi) {
        resize(width, height, densityDpi, true /* doLayout */);
    }

    private void resize(int width, int height, int densityDpi, boolean doLayout) {
        mWidth = width;
        mHeight = height;
        mDensityDpi = densityDpi;

        if (doLayout) {
            relayout();
        }
    }

    public void relayout() {
        if (mWindowVisible) {
            updateWindowParams();
            mWindowManager.updateViewLayout(mWindowContent, mWindowParams);
        }
    }

    private void createWindow() {
        LayoutInflater inflater = LayoutInflater.from(DemoApplication.getApp());

        mWindowContent = inflater.inflate(R.layout.overlay_display_window, null);
        mWindowContent.setOnTouchListener(mOnTouchListener);

        mTcpipImageView = mWindowContent.findViewById(R.id.overlay_display_window_tcpip);
        mTcpipImageView.setOnClickListener(mTcpipListener);

        mPairImageView = mWindowContent.findViewById(R.id.overlay_display_window_pair);
        mPairImageView.setOnClickListener(mPairListener);

        mHideImageView = mWindowContent.findViewById(R.id.overlay_display_window_hide);
        mHideImageView.setOnClickListener(mHideListener);

        mLockImageView = mWindowContent.findViewById(R.id.overlay_display_window_lock);
        mLockImageView.setOnClickListener(mLockListener);

        mFocusImageView = mWindowContent.findViewById(R.id.overlay_display_window_focus);
        mFocusImageView.setOnClickListener(mFocusListener);

        mRemarkTextView = mWindowContent.findViewById(R.id.overlay_display_window_remark);

        if (secondaryVirtualDisplayReady()) {
            Log.i(TAG, "secondaryVirtualDisplayReady");
            mTcpipImageView.setVisibility(View.GONE);
            mPairImageView.setVisibility(View.GONE);
            mRemarkTextView.setVisibility(View.GONE);
            mLockImageView.setVisibility(View.VISIBLE);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(DemoApplication.getApp().getString(R.string.jar_not_started));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                mPairImageView.setVisibility(View.GONE);
            } else {
                sb.append(DemoApplication.getApp().getString(R.string.adb_pair_remark));

                int adbWifiEnabled = Settings.Global.getInt(DemoApplication.getApp().getContentResolver(), "adb_wifi_enabled", 0);
                Log.i(TAG, "adbWifiEnabled: " + adbWifiEnabled);
            }
            sb.append(DemoApplication.getApp().getString(R.string.adb_tcpip_remark));

            String remark = sb.toString();
            Log.i(TAG, "remark: " + remark);

            mRemarkTextView.setText(remark);
        }

        mTextureView = mWindowContent.findViewById(R.id.overlay_display_window_texture);
        mTextureView.setPivotX(0);
        mTextureView.setPivotY(0);
        mTextureView.getLayoutParams().width = mWidth;
        mTextureView.getLayoutParams().height = mHeight;
        mTextureView.setOpaque(false);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        mWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        mWindowParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        if (ADD_FLAG_SECURE) {
            mWindowParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        }
        if (DISABLE_MOVE_AND_RESIZE) {
            mWindowParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        mWindowParams.alpha = WINDOW_ALPHA;
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        mGestureDetector = new GestureDetector(DemoApplication.getApp(), mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(DemoApplication.getApp(), mOnScaleGestureListener);

        mWindowX = 0;
        mWindowY = 0;
        mWindowScale = INITIAL_SCALE;
    }

    private void updateWindowParams() {
        float scale = mWindowScale * mLiveScale;
        scale = Math.min(scale, 1.0f);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));

        float offsetScale = (scale / mWindowScale - 1.0f) * 0.5f;
        int width = (int)(mWidth * scale);
        int height = (int)(mHeight * scale);
        int x = (int)(mWindowX + mLiveTranslationX - width * offsetScale);
        int y = (int)(mWindowY + mLiveTranslationY - height * offsetScale);
        x = Math.max(0, Math.min(x, mWidth - width));
        y = Math.max(0, Math.min(y, mHeight - height));

        mTextureView.setScaleX(scale);
        mTextureView.setScaleY(scale);

        mWindowParams.x = x;
        mWindowParams.y = y;
        mWindowParams.width = width;
        mWindowParams.height = height;

        mRealScale = scale;
    }

    private void saveWindowParams() {
        mWindowX = mWindowParams.x;
        mWindowY = mWindowParams.y;
        mWindowScale = mTextureView.getScaleX();
        clearLiveState();
    }

    private void clearLiveState() {
        mLiveTranslationX = 0f;
        mLiveTranslationY = 0f;
        mLiveScale = 1.0f;
    }

    public View getWindowContent() {
        return mWindowContent;
    }

    private final View.OnClickListener mTcpipListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFloatDialog.show(false);
                    focusImageViewShow(true);
                }
            };

    private final View.OnClickListener mPairListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "mPairListener");
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        DemoApplication.getApp().startActivity(intent);

                        mFloatDialog.show(true);
                        focusImageViewShow(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    private final View.OnClickListener mHideListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFloatIcon.show();
                }
            };

    private final View.OnClickListener mFocusListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isFocused = !isFocused;
                    focusImageViewChange(isFocused);
                }
            };
    private final View.OnClickListener mLockListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isLocked = !isLocked;
                    if (isLocked) {
                        if (mControlClient == null) {
                            mControlClient = new ControlClient();
                        }

                        mLockImageView.setImageResource(R.drawable.go_lock);

                        focusImageViewShow(true);

                        if (USE_SURFACE_EVENT) {
                            mTextureView.setOnTouchListener((view, event) -> {
                                    if (mControlClient !=  null) {
                                        ControlClient.offerEvent(event);
                                    }
                                    return true;
                            });
                        }
                    } else {
                        mLockImageView.setImageResource(R.drawable.go_unlock);

                        isFocused = false;
                        focusImageViewShow(false);

                        if (USE_SURFACE_EVENT) {
                            mTextureView.setOnTouchListener(null);
                        }
                    }
                }
            };

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId == mDisplayId) {
                        relayout();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    if (displayId == mDisplayId) {
                        dismiss();
                    }
                }
            };

    private final SurfaceTextureListener mSurfaceTextureListener =
            new SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    Log.i(TAG, "onSurfaceTextureAvailable width:" + width + " height:" + height);

                    if (USE_SELF_VIRTUALDISPLAY) {
                        String name = "virtualdisplay";
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            name = "PC_virtualdisplay";
                        }
                        VirtualDisplay virtualDisplay = mDisplayManager.createVirtualDisplay(name, width, height, mDensityDpi, new Surface(surfaceTexture), DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, null, null);

                        Display display = virtualDisplay.getDisplay();
                        Log.i(TAG, "FloatWindow display: " + display);
                    } else {
                        if (View.VISIBLE == mLockImageView.getVisibility()) {
                            mVideoClient = new VideoClient();
                            mVideoClient.start(new Surface(surfaceTexture));
                        } else {
                            mSurfaceTexture = surfaceTexture;
                        }
                    }
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                }
            };
    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (!USE_SURFACE_EVENT && isLocked) {
                if (mControlClient != null) {
                    event.setLocation(event.getX()/mRealScale, event.getY()/mRealScale);

                    ControlClient.offerEvent(event);
                }
                return true;
            }

            // Work in screen coordinates.
            final float oldX = event.getX();
            final float oldY = event.getY();
            event.setLocation(event.getRawX(), event.getRawY());

            mGestureDetector.onTouchEvent(event);
            mScaleGestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    saveWindowParams();
                    break;
            }

            // Revert to window coordinates.
            event.setLocation(oldX, oldY);
            return true;
        }
    };

    private final GestureDetector.OnGestureListener mOnGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    mLiveTranslationX -= distanceX;
                    mLiveTranslationY -= distanceY;
                    relayout();
                    return true;
                }
            };

    private final ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener =
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    mLiveScale *= detector.getScaleFactor();
                    relayout();
                    return true;
                }
            };

    private boolean secondaryVirtualDisplayReady() {
        Display[] displays = mDisplayManager.getDisplays();
        if (displays.length > 1) {
            for (Display display : displays) {
                if (display.getName().contains("virtualdisplay")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void serverReady() {
        Log.i(TAG, "serverReady");
        try {
            boolean ready = false;
            for (int i = 0; i < 3; i++) {
                ready = secondaryVirtualDisplayReady();
                if (ready || i == 2) {
                    break;
                }
                Log.i(TAG, "serverReady wait 1s");
                Thread.sleep(1000);
            }

            if (ready) {
                mAdbDebug.disconnect();

                if (!USE_SELF_VIRTUALDISPLAY
                        && mVideoClient == null
                        && mSurfaceTexture != null) {
                    mLockImageView.setVisibility(View.VISIBLE);
                    mPairImageView.setVisibility(View.GONE);
                    mTcpipImageView.setVisibility(View.GONE);
                    mRemarkTextView.setVisibility(View.GONE);

                    mVideoClient = new VideoClient();
                    mVideoClient.start(new Surface(mSurfaceTexture));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void focusImageViewChange(boolean focus) {
        if (focus) {
            mFocusImageView.setImageResource(R.drawable.focus_strong);

            mWindowParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            mFocusImageView.setImageResource(R.drawable.focus_weak);

            mWindowParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        mWindowManager.updateViewLayout(mWindowContent, mWindowParams);
    }

    public void focusImageViewShow(boolean show) {
        mFocusImageView.setVisibility(show ? View.VISIBLE : View.GONE);
        focusImageViewChange(false);
    }
}