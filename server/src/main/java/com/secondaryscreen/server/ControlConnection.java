package com.secondaryscreen.server;

import android.os.Parcel;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

public final class ControlConnection extends ServerChannel {
    private static int PORT = 8402;
    private long lastTouchDown;
    private MotionEvent.PointerProperties[] pointerProperties;
    private MotionEvent.PointerCoords[] pointerCoords;
    public ControlConnection() {
        super(PORT);
    }

    @Override
    public void work(byte[] buffer, int length) {
        MotionEvent event = null;
        if (Utils.isSingleMachineMode()) {
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(buffer, 0, length);
            parcel.setDataPosition(0);
            event = MotionEvent.CREATOR.createFromParcel(parcel);
            parcel.recycle();
        } else {
            initMotionEvent();
            String eventMessage = new String(buffer, 0, length);
            event = createMotionEvent(eventMessage);
        }

        if (event != null) {
            InputManager.setDisplayId(event, DisplayInfo.getMirrorDisplayId());
            ServiceManager.getInputManager().injectInputEvent(event);
        }
    }

    private void initMotionEvent() {
        if (pointerProperties == null || pointerCoords == null) {
            pointerProperties = new MotionEvent.PointerProperties[10];
            pointerCoords = new MotionEvent.PointerCoords[10];
            for (int i = 0; i < 10; i++) {
                pointerProperties[i] = new MotionEvent.PointerProperties();
                pointerProperties[i].id = i;
                pointerProperties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
                pointerCoords[i] = new MotionEvent.PointerCoords();
                pointerCoords[i].x = 0;
                pointerCoords[i].y = 0;
                pointerCoords[i].pressure = 1.0f;
                pointerCoords[i].size = 1.0f;
            }
        }
    }

    private MotionEvent createMotionEvent(String eventMessage) {
        System.out.println("eventMessage:" + eventMessage);
        String[] splited = eventMessage.split(";");
        int action = Integer.parseInt(splited[0]);
        int pointerCount = Integer.parseInt(splited[1]);
        for (int i = 0; i < pointerCount; i++) {
            String[] pointers = splited[i + 2].split(",");
            pointerProperties[i].id = Integer.parseInt(pointers[0]);
            pointerCoords[i].x = Float.parseFloat(pointers[1]);
            pointerCoords[i].y = Float.parseFloat(pointers[2]);
            pointerCoords[i].pressure = action == MotionEvent.ACTION_UP ? 0.0f : 1.0f;
        }
        long now = SystemClock.uptimeMillis();
        if (action == MotionEvent.ACTION_DOWN) {
            lastTouchDown = now;
        }

        MotionEvent event = MotionEvent.obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        return event;
    }
}
