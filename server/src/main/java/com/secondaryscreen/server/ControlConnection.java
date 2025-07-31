package com.secondaryscreen.server;

import android.os.Parcel;
import android.util.Log;
import android.view.MotionEvent;

public final class ControlConnection extends ServerChannel {
    private static final String TAG = "ControlConnection";
    public ControlConnection() {
        super(Utils.CONTROL_CHANNEL_PORT);
    }

    @Override
    public void work(byte[] buffer, int length) {
        int displayId = Utils.byte4ToInt(buffer);
        Log.i(TAG, "displayId:" + displayId);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(buffer, 4, length - 4);
        parcel.setDataPosition(0);
        MotionEvent event = MotionEvent.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        InputManager.setDisplayId(event, displayId);
        ServiceManager.getInputManager().injectInputEvent(event);
    }
}
