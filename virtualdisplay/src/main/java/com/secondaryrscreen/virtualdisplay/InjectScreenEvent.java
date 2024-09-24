package com.secondaryscreen.virtualdisplay;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InjectScreenEvent {
    private static int PORT = 8404;
    private static String HOST = "127.0.0.1";

    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;

    private static Object INPUT_MANAGER;
    private static Method InjectInputEventMethod;

    static void create() {
        try {
            Class<?> inputManagerClass = getInputManagerClass();
            Method getInstanceMethod = inputManagerClass.getDeclaredMethod("getInstance");
            INPUT_MANAGER = getInstanceMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Class<?> getInputManagerClass() {
        try {
            // Parts of the InputManager class have been moved to a new InputManagerGlobal class in Android 14 preview
            return Class.forName("android.hardware.input.InputManagerGlobal");
        } catch (ClassNotFoundException e) {
            return android.hardware.input.InputManager.class;
        }
    }

    private static Method getInjectInputEventMethod() throws NoSuchMethodException {
        if (InjectInputEventMethod == null) {
            InjectInputEventMethod = INPUT_MANAGER.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        }
        return InjectInputEventMethod;
    }

    public static boolean injectInputEvent(InputEvent inputEvent) {
        try {
            Method method = getInjectInputEventMethod();
            return (boolean) method.invoke(INPUT_MANAGER, inputEvent, INJECT_INPUT_EVENT_MODE_ASYNC);
        } catch (ReflectiveOperationException e) {
            System.out.println("Could not invoke method:" + e);
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            create();

            Thread screenEventClientThread = new ScreenEventClientThread();
            screenEventClientThread.start();

            screenEventClientThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ScreenEventClientThread extends Thread {
        int[] xy = new int[2];

        boolean[] waitForXY = {true, true};

        private long lastTouchDown;

        boolean recvDown = false;

        public ScreenEventClientThread() {
            super("ScreenEventClientThread");
            System.out.println("ScreenEventClientThread");
        }

        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(HOST, PORT), 3000);

                System.out.println("ScreenEventClientThread connect");

                byte[] eventBuffer = new byte[0];
                byte[] lengthBuffer = new byte[4];
                int len = 0;
                InputStream inputStream = socket.getInputStream();

                while (!Thread.currentThread().isInterrupted()) {
                    recv(inputStream, lengthBuffer, lengthBuffer.length);

                    len = byte4ToInt(lengthBuffer);
                    if (eventBuffer.length < len) {
                        System.out.println("eventBuffer.length:" + eventBuffer.length + " < len:" + len);
                        eventBuffer = new byte[len];
                    }
                    recv(inputStream, eventBuffer, len);

                    String line = new String(eventBuffer, 0, len);

                    System.out.println(line);

                    if (line.contains("BTN_TOUCH")) {
                        if (line.contains("UP")) {
                            recvDown = false;
                            inject(MotionEvent.ACTION_UP, xy[0], xy[1]);
                        } else if (line.contains("DOWN")) {
                            recvDown = true;
                            inject(MotionEvent.ACTION_DOWN, xy[0], xy[1]);
                            move();
                        }
                    } else if (line.contains("ABS_MT_POSITION_X")) {
                        String[] splited = line.split("ABS_MT_POSITION_X");
                        String x = splited[splited.length - 1].trim();
                        xy[0] = (int) (Integer.parseInt(x, 16) * 0.1f);

                        waitForXY[0] = false;

                        if (recvDown) {
                            move();
                        }
                    } else if (line.contains("ABS_MT_POSITION_Y")) {
                        String[] splited = line.split("ABS_MT_POSITION_Y");
                        String y = splited[splited.length - 1].trim();
                        xy[1] = (int) (Integer.parseInt(y, 16) * 0.1f);

                        waitForXY[1] = false;

                        if (recvDown) {
                            move();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("socket exception:" + e);
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void move() {
            if (!waitForXY[0] && !waitForXY[1]) {
                inject(MotionEvent.ACTION_MOVE, xy[0], xy[1]);

                // 接收下一次事件
                waitForXY[0] = true;
                waitForXY[1] = true;
            }
        }

        private static void recv(InputStream inputStream, byte[] buffer, int sum) throws Exception {
            int read = 0;
            while (sum - read > 0) {
                int len = inputStream.read(buffer, read, sum - read);
                if (len == -1) {
                    throw new RuntimeException("socket closed");
                }
                read += len;
            }
        }

        public static int byte4ToInt(byte[] bytes) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            int b2 = bytes[2] & 0xFF;
            int b3 = bytes[3] & 0xFF;
            return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        }

        public void inject(int action, int x, int y) {
            System.out.println("x:" + x);
            System.out.println("y:" + y);

            long now = SystemClock.uptimeMillis();

            final int pointerCount = 1;
            MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[pointerCount];
            MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[pointerCount];
            for (int i = 0; i < pointerCount; i++) {
                pointerProperties[i] = new MotionEvent.PointerProperties();
                pointerProperties[i].id = i;
                pointerProperties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
                pointerCoords[i] = new MotionEvent.PointerCoords();
                pointerCoords[i].x = x;
                pointerCoords[i].y = y;
                pointerCoords[i].pressure = action == MotionEvent.ACTION_UP ? 0.0f : 1.0f;
                pointerCoords[i].size = 1.0f;
            }

            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }

            MotionEvent event = MotionEvent.obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            injectInputEvent(event);
            System.out.println("injectInputEvent:" + event);
        }
    }
}