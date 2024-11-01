package com.overlaywindow.demo;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Method;

// 部分逻辑参考自：
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/display/OverlayDisplayWindow.java

final class FloatDialog {
    private static final String TAG = "FloatIcon";
    private boolean mIsPair;
    private final View mDialogView;
    private final EditText mPairingCodeEditText;
    private final EditText mPortNumberEditText;
    private final TextView mStatusTextView;
    private final FloatWindow mFloatWindow;
    public FloatDialog(FloatWindow floatWindow) {
        mFloatWindow = floatWindow;

        mDialogView = floatWindow.getWindowContent().findViewById(R.id.overlay_display_window_input);
        mDialogView.setVisibility(View.GONE);

        mStatusTextView = mDialogView.findViewById(R.id.overlay_display_input_status);

        mPairingCodeEditText = mDialogView.findViewById(R.id.overlay_display_input_pair);
        mPortNumberEditText = mDialogView.findViewById(R.id.overlay_display_input_port);

        mDialogView.findViewById(R.id.connect_button).setOnClickListener(mConnectListener);
        mDialogView.findViewById(R.id.cancel_button).setOnClickListener(mCancelListener);
    }

    public void show(boolean isPair) {
        mIsPair = isPair;
        mDialogView.setVisibility(View.VISIBLE);

        mPairingCodeEditText.setText("");
        mStatusTextView.setText(R.string.adb_disconnect);

        if (mIsPair) {
            ((TextView)mDialogView.findViewById(R.id.overlay_display_input_title)).setText(R.string.adb_pair_title);
            mDialogView.findViewById(R.id.overlay_display_input_row_pair).setVisibility(View.VISIBLE);
            ((TextView)mDialogView.findViewById(R.id.connect_button)).setText(R.string.pair);
            mPortNumberEditText.setText("");
        } else {
            ((TextView)mDialogView.findViewById(R.id.overlay_display_input_title)).setText(R.string.adb_tcpip_title);
            mDialogView.findViewById(R.id.overlay_display_input_row_pair).setVisibility(View.GONE);
            ((TextView)mDialogView.findViewById(R.id.connect_button)).setText(R.string.connect);

            String port = "";
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method get = c.getMethod("get", String.class, String.class);
                port = (String)(get.invoke(c, "service.adb.tcp.port", port));
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPortNumberEditText.setText(port);
        }

        AdbShell.getInstance().getPairingPort(() -> {
            mPortNumberEditText.setText(String.valueOf(AdbShell.getInstance().getPort()));
        });
    }
    private void hide() {
        mDialogView.setVisibility(View.GONE);
        mFloatWindow.focusImageViewShow(false);

        mFloatWindow.serverReady();
    }

    private final View.OnClickListener mConnectListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CharSequence portNumberString = mPortNumberEditText.getText();

                    if (mIsPair) {
                        CharSequence pairingCode = mPairingCodeEditText.getText();

                        if (pairingCode != null
                                && pairingCode.length() == 6
                                && portNumberString != null
                                && portNumberString.length() > 0
                                && TextUtils.isDigitsOnly(portNumberString)) {
                            mStatusTextView.setText(R.string.adb_connecting);

                            int port = Integer.parseInt(portNumberString.toString());
                            AdbShell.getInstance().pair(port, pairingCode.toString(), () -> {
                                if (AdbShell.getInstance().getConnectStatus()) {
                                    mStatusTextView.setText(R.string.adb_connect_success);

                                    hide();
                                } else {
                                    mStatusTextView.setText(R.string.adb_connect_failed);
                                }
                            });
                        }
                    } else {
                        if (portNumberString != null
                                && portNumberString.length() > 0
                                && TextUtils.isDigitsOnly(portNumberString)) {
                            mStatusTextView.setText(R.string.adb_connecting);

                            int port = Integer.parseInt(portNumberString.toString());
                            AdbShell.getInstance().connect(port, () -> {
                                if (AdbShell.getInstance().getConnectStatus()) {
                                    mStatusTextView.setText(R.string.adb_connect_success);

                                    hide();
                                } else {
                                    mStatusTextView.setText(R.string.adb_connect_failed);
                                }
                            });
                        }
                    }
                }
            };

    private final View.OnClickListener mCancelListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialogView.setVisibility(View.GONE);
                    mFloatWindow.focusImageViewShow(false);
                }
            };
}