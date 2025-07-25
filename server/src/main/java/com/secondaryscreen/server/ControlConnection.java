package com.secondaryscreen.server;

import android.os.Parcel;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

public final class ControlConnection extends ServerChannel {
    public ControlConnection() {
        super(Utils.CONTROL_CHANNEL_PORT);
    }

    @Override
    public void work(byte[] buffer, int length) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(buffer, 0, length);
        parcel.setDataPosition(0);
        MotionEvent event = MotionEvent.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        InputManager.setDisplayId(event, DisplayInfo.getDisplayId());
        ServiceManager.getInputManager().injectInputEvent(event);
    }
}
