package com.secondaryscreen.virtualdisplay;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class PlaybackScreenEvent {
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

        final int pointerCount = 1;
        MotionEvent.PointerProperties[] pointerProperties;
        MotionEvent.PointerCoords[] pointerCoords;

        public ScreenEventClientThread() {
            super("ScreenEventClientThread");
            System.out.println("ScreenEventClientThread");
        }

        @Override
        public void run() {
            try {
                pointerProperties = new MotionEvent.PointerProperties[pointerCount];
                pointerCoords = new MotionEvent.PointerCoords[pointerCount];
                for (int i = 0; i < pointerCount; i++) {
                    pointerProperties[i] = new MotionEvent.PointerProperties();
                    pointerProperties[i].id = i;
                    pointerProperties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
                    pointerCoords[i] = new MotionEvent.PointerCoords();
                    pointerCoords[i].x = 0;
                    pointerCoords[i].y = 0;
                    pointerCoords[i].pressure = 1.0f;
                    pointerCoords[i].size = 1.0f;
                }

                while (true) {
                    File file = new File("/data/local/tmp/playback.txt");
                    InputStream inputStream = new FileInputStream(file);
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    String line;
                    long lastTime = 0;
                    long time = 0;
                    while ((line = bufferedReader.readLine()) != null) {
                        time = getEventMicroSecond(line);
                        if (lastTime == 0) {
                            lastTime = time;
                        } else {
                            long diff = time - lastTime;
                            if (diff > 1000) {
                                Thread.sleep(diff / 1000);
                                lastTime = time;
                            }
                        }

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

                    inputStream.close();

                    Thread.sleep(3000)  ;
                }
            } catch (Exception e) {
                System.out.println("socket exception:" + e);
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

        public void inject(int action, int x, int y) {
            System.out.println("x:" + x);
            System.out.println("y:" + y);

            long now = SystemClock.uptimeMillis();

            // Physical size: 1200x1920
            pointerCoords[0].x = x;
            pointerCoords[0].y = y;
            pointerCoords[0].pressure = action == MotionEvent.ACTION_UP ? 0.0f : 1.0f;
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }

            MotionEvent event = MotionEvent.obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            injectInputEvent(event);
            System.out.println("injectInputEvent:" + event);
        }

        private long getEventMicroSecond(String line) {
            String content = line.split("]")[0].trim();
            content = content.substring(1).replace(".", "");
            return Long.parseLong(content.trim());
        }
    }
}