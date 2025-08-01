package com.secondaryscreen.app;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
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
    private final float WINDOW_ALPHA = 1.0f;
    private final boolean DISABLE_MOVE_AND_RESIZE = false;
    private final boolean ADD_FLAG_SECURE = false;
    private final boolean USE_SURFACE_EVENT = false;
    private final boolean USE_APP_VIRTUALDISPLAY = false;
    private int mWidth;
    private int mHeight;
    private int mDensityDpi;
    private DisplayManager mDisplayManager;
    private WindowManager mWindowManager;
    private View mWindowContent;
    private ImageView mLockImageView;
    private ImageView mFocusImageView;
    private WindowManager.LayoutParams mWindowParams;
    private TextureView mTextureView;
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
    private boolean mIsLocked = false;
    private boolean mIsFocused = false;
    private ControlConnection mControlConnection;
    private VideoConnection mVideoConnection;
    private DisplayConnection mDisplayConnection;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mRotation;

    public FloatWindow() {
        mDisplayManager = (DisplayManager)Utils.getContext().getSystemService(
                Context.DISPLAY_SERVICE);
        mWindowManager = (WindowManager)Utils.getContext().getSystemService(
                Context.WINDOW_SERVICE);

        Display display = mWindowManager.getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        mScreenWidth = realMetrics.widthPixels;
        mScreenHeight = realMetrics.heightPixels;

        mRotation = display.getRotation();

        if (mRotation % 2 == 0) {
            resize(Resolution.R.TEXTUREVIEW_WIDTH,
                    Resolution.R.TEXTUREVIEW_HEIGHT,
                    Resolution.R.VIRTUALDISPLAY_DENSITYDPI,
                    false);
        } else {
            resize(Resolution.R.TEXTUREVIEW_HEIGHT,
                    Resolution.R.TEXTUREVIEW_WIDTH,
                    Resolution.R.VIRTUALDISPLAY_DENSITYDPI,
                    false);
        }
        createWindow();

        mFloatIcon = new FloatIcon(mWindowContent);

        mVideoConnection = new VideoConnection();
        mControlConnection = new ControlConnection();
        mDisplayConnection = new DisplayConnection();
        mDisplayConnection.setScreenInfo(0,
                Resolution.R.VIRTUALDISPLAY_WIDTH,
                Resolution.R.VIRTUALDISPLAY_HEIGHT,
                Resolution.R.VIRTUALDISPLAY_DENSITYDPI,
                mRotation);
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

            MainActivity.clearFloatWindow();

            System.exit(0);
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
        LayoutInflater inflater = LayoutInflater.from(Utils.getContext());

        mWindowContent = inflater.inflate(R.layout.overlay_display_window, null);
        mWindowContent.setOnTouchListener(mOnTouchListener);

        mWindowContent.findViewById(R.id.overlay_display_window_close).setOnClickListener((View v) -> {
            dismiss();
        });

        mWindowContent.findViewById(R.id.overlay_display_window_shrink).setOnClickListener((View v) -> {
            mFloatIcon.show();
        });

        mLockImageView = mWindowContent.findViewById(R.id.overlay_display_window_lock);
        mLockImageView.setOnClickListener(mLockListener);

        mFocusImageView = mWindowContent.findViewById(R.id.overlay_display_window_focus);
        mFocusImageView.setOnClickListener((View v) -> {
            mIsFocused = !mIsFocused;
            focusImageViewChange(mIsFocused);
        });

        TextView titleTextView = mWindowContent.findViewById(R.id.overlay_display_window_title);
        titleTextView.append(" " + Resolution.R.toSimpleString());

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

        mGestureDetector = new GestureDetector(Utils.getContext(), mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(Utils.getContext(), mOnScaleGestureListener);

        mWindowX = 0;
        mWindowY = 0;
        mWindowScale = INITIAL_SCALE;
    }

    private void updateWindowParams() {
        float scale = mWindowScale * mLiveScale;
        scale = Math.min(scale, (float)mScreenWidth / mWidth);
        scale = Math.min(scale, (float)mScreenHeight / mHeight);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));

        float offsetScale = (scale / mWindowScale - 1.0f) * 0.5f;
        int width = (int)(mWidth * scale);
        int height = (int)(mHeight * scale);
        int x = (int)(mWindowX + mLiveTranslationX - width * offsetScale);
        int y = (int)(mWindowY + mLiveTranslationY - height * offsetScale);
        x = Math.max(0, Math.min(x, mScreenWidth - width));
        y = Math.max(0, Math.min(y, mScreenHeight - height));


        mTextureView.setScaleX(scale);
        mTextureView.setScaleY(scale);

        mWindowParams.x = x;
        mWindowParams.y = y;
        mWindowParams.width = width;
        mWindowParams.height = height;

        mRealScale = scale * Resolution.R.SCALE_X;
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
    
    private final View.OnClickListener mLockListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mIsLocked = !mIsLocked;
                    if (mIsLocked) {
                        mLockImageView.setImageResource(R.drawable.go_lock);

                        focusImageViewShow(true);

                        if (USE_SURFACE_EVENT) {
                            mTextureView.setOnTouchListener((view, event) -> {
                                if (mIsLocked) {
                                    Utils.offerMotionEvent(event, true);
                                }
                                return true;
                            });
                        }
                    } else {
                        mLockImageView.setImageResource(R.drawable.go_unlock);

                        mIsFocused = false;
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
                    Log.i(TAG, "onDisplayChanged:" + displayId);
                    if (displayId == 0) {
                        int rotation = Utils.getRotation();
                        if (mRotation != rotation) {
                            onRotationChanged(rotation);
                        }
                    } else {
                        relayout();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    Log.i(TAG, "onDisplayRemoved:" + displayId);

                    if (displayId != 0 && !Utils.checkVirtualDisplayReady()) {
                        dismiss();
                    }
                }
            };

    private final SurfaceTextureListener mSurfaceTextureListener =
            new SurfaceTextureListener() {
                private VirtualDisplay mVirtualDisplay = null;
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    Log.i(TAG, "onSurfaceTextureAvailable surfaceTexture:" + surfaceTexture + " width:" + width + " height:" + height);

                    if (USE_APP_VIRTUALDISPLAY) {
                        /** remark
                         * 可以在APP内部直接创建virtualdisplay
                         * server端只需要对接control通道完成事件注入即可
                         * server端无需对接video通道
                         * server端无需对接display通道
                         * APP侧在屏幕旋转时需要处理onSurfaceTextureSizeChanged回调
                         *
                         * 此方式启动的virtaldisplay在APP完全退出时，无法进行scrcpy投屏
                         * TODO 创建一个新的分支，在新分支中实现此方式的投屏
                         */
                        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            flags = flags | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | (1 << 10);
                        }
                        mVirtualDisplay = mDisplayManager.createVirtualDisplay(Utils.VIRTUALDISPLAY_NAME, width, height, mDensityDpi, new Surface(surfaceTexture), flags, null, null);
                        Display display = mVirtualDisplay.getDisplay();
                        Log.i(TAG, "FloatWindow display: " + display);
                    } else {
                        if (View.VISIBLE == mLockImageView.getVisibility()) {
                            mVideoConnection.start(new Surface(surfaceTexture), mDisplayConnection);
                            mControlConnection.start();
                        }
                    }
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    // Log.v(TAG, "onSurfaceTextureDestroyed");
                    return true;
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                    Log.i(TAG, "onSurfaceTextureSizeChanged surfaceTexture:" + surfaceTexture + " width:" + width + " height:" + height);
                    if (USE_APP_VIRTUALDISPLAY && mVirtualDisplay != null) {
                        mVirtualDisplay.resize(width, height, mDensityDpi);
                    }
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                    // Log.v(TAG, "onSurfaceTextureUpdated");
                }
            };
    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (mIsLocked) {
                if (!USE_SURFACE_EVENT) {
                    event.setLocation(event.getX() / mRealScale, event.getY() / mRealScale);

                    Utils.offerMotionEvent(event, true);
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

    private void onRotationChanged(int rotation) {
        mDisplayConnection.setScreenInfo(0,
                Resolution.R.VIRTUALDISPLAY_WIDTH,
                Resolution.R.VIRTUALDISPLAY_HEIGHT,
                Resolution.R.VIRTUALDISPLAY_DENSITYDPI,
                rotation);

        if ((mRotation + rotation) % 2 != 0) {
            int tmp = mScreenWidth;
            mScreenWidth = mScreenHeight;
            mScreenHeight = tmp;

            if (rotation % 2 == 0) {
                mTextureView.getLayoutParams().width = mWidth = Resolution.R.TEXTUREVIEW_WIDTH;
                mTextureView.getLayoutParams().height = mHeight = Resolution.R.TEXTUREVIEW_HEIGHT;
            } else {
                mTextureView.getLayoutParams().width = mWidth = Resolution.R.TEXTUREVIEW_HEIGHT;
                mTextureView.getLayoutParams().height = mHeight = Resolution.R.TEXTUREVIEW_WIDTH;
            }

            Log.i(TAG, "onRotationChanged width:" + mWidth + " height:" + mHeight);

            relayout();
        }

        mRotation = rotation;
    }
}
