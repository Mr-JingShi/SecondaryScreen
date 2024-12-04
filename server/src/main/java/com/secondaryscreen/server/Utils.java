package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.TaskInfo;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static String TAG = "Utils";
    static final String APP_PACKAGE_NAME = "com.secondaryscreen.app";
    static final String APP_MAIN_ACTIVITY_CLASS_NAME = APP_PACKAGE_NAME + ".MainActivity";
    static final String APP_MAIN_ACTIVITY_NAME = APP_PACKAGE_NAME + "/" + APP_MAIN_ACTIVITY_CLASS_NAME;
    static final String APP_SECOND_ACTIVITY_CLASS_NAME = APP_PACKAGE_NAME + ".SecondActivity";
    static final String APP_SECOND_ACTIVITY_NAME = APP_PACKAGE_NAME + "/" + APP_SECOND_ACTIVITY_CLASS_NAME;
    static final String PACKAGE_NAME = "com.android.shell";
    static final String VIRTUALDISPLAY_NAME = "secondaryscreen";
    static int CONTROL_CHANNEL_PORT = 8402;
    static int VIDEO_CHANNEL_PORT = 8403;
    static int DISPLAY_CHANNEL_PORT = 8404;
    private static boolean mIsSingleMachineMode = true;
    private static ScheduledExecutorService mExecutor  = Executors.newSingleThreadScheduledExecutor();

    static boolean isSingleMachineMode() {
        return mIsSingleMachineMode;
    }
    static void setSingleMachineMode(boolean isSingleMachineMode) {
        mIsSingleMachineMode = isSingleMachineMode;
    }

    static List<Pair<Boolean, Boolean>> checkActivityReadyByShell(@NonNull List<Pair<String, String>> lists) {
        try {
            // Stack id=535 bounds=[0,0][1080,2340] displayId=38 userId=0
            //  configuration={1.0 ?mcc?mnc [zh_CN] ldltr sw392dp w392dp h803dp 440dpi nrml long port night finger -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1080, 2340) mAppBounds=Rect(0, 0 - 1080, 2210) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=home mAlwaysOnTop=undefined mRotation=ROTATION_0} s.3321 themeChanged=0 themeChangedFlags=0 extraData = Bundle[{}]}
            //   taskId=3739: com.miui.home/com.miui.home.launcher.SecondaryDisplayLauncher bounds=[0,0][1080,2340] userId=0 visible=true topActivity=ComponentInfo{com.miui.home/com.miui.home.launcher.SecondaryDisplayLauncher}
            String cmd = "am stack list";
            String dump = Shell.execReadOutput("sh", "-c", cmd);
            String[] lines = dump.split("\\n");

            List<ActivityManager.StackInfo> stackInfos = new ArrayList<>();
            ActivityManager.StackInfo stackInfo = null;
            for (String line : lines) {
                if (!line.isEmpty()) {
                    if (line.contains("Stack id=") || line.contains("RootTask id=")) {
                        int index1 = line.indexOf("displayId=");
                        int index2 = line.indexOf(" userId=");
                        stackInfo = new ActivityManager.StackInfo();
                        stackInfo.displayId = Integer.parseInt(line.substring(index1 + 10, index2));
                        stackInfo.taskNames = new ArrayList<>();
                        stackInfos.add(stackInfo);
                    } else if (line.contains("taskId=")) {
                        int index1 = line.indexOf(" ", 7);
                        int index2 = line.indexOf(" ", index1 + 1);
                        String activityName = line.substring(index1 + 1, index2);
                        stackInfo.taskNames.add(activityName);
                    }
                }
            }

            Ln.i(TAG, "StackInfos size:" + stackInfos.size());
            return checkStackInfo(lists, stackInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void startActivity(@NonNull String activity, int displayId) {
        StringBuilder sb = new StringBuilder();

        sb.append("am start -n ");
        sb.append(activity);
        sb.append(" --display ");
        sb.append(displayId);

        String text = sb.toString();
        try {
            Shell.exec("sh", "-c", text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String prettifyActivity(@NonNull String activity) {
        int index = activity.indexOf ("/.");
        if (index > 0) {
            String prefix = activity.substring(0, index);
            activity = activity.replace("/", "/" + prefix);
        }
        return activity;
    }

    static Class<?> findClass(Class<?>[] innerClasses, String name) throws ClassNotFoundException {
        for (Class<?> clazz : innerClasses) {
            if (clazz.getName().equals(name)) {
                return clazz;
            }
        }
        throw new ClassNotFoundException(name);
    }

    static Method findMethodAndMakeAccessible(Method[] methods, String name) throws NoSuchMethodException {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    static void schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        mExecutor.schedule(runnable, time, timeUnit);
    }

    static void shutdown() {
        mExecutor.shutdown();
    }

    @RequiresApi(api = 29)
    static List<Pair<Boolean, Boolean>> checkActivityReady(@NonNull List<Pair<String, String>> lists) {
        try {
            List<ActivityManager.StackInfo> stackInfos = ServiceManager.getActivityManager().getAllTaskInfos();
            Ln.i(TAG, "StackInfos size:" + stackInfos.size());

            return checkStackInfo(lists, stackInfos);
        } catch (ReflectiveOperationException e) {
            Ln.e(TAG, "getAllTaskInfos Could not invoke method", e);
        }

        try {
            List<TaskInfo> taskInfos = ServiceManager.getActivityManager().getTasks(9999);
            Ln.i(TAG, "TaskInfos size:" + taskInfos.size());

            List<Pair<Boolean, Boolean>> result = new ArrayList<>();
            for (int i = 0; i < lists.size(); ++i) {
                Pair<String, String> pair = lists.get(i);
                String firstActivity = pair.first;
                String secondActivity = pair.second;
                boolean firstActivityStarted = false;
                boolean secondResultStarted = false;
                if (firstActivity == null) {
                    firstActivityStarted = true;
                }

                for (TaskInfo taskInfo : taskInfos) {
                    if (taskInfo.baseIntent.getComponent() != null) {
                        String packageName = taskInfo.baseIntent.getComponent().getPackageName();
                        String className = taskInfo.baseIntent.getComponent().getClassName();
                        String activityName = packageName + "/" + className;
                        Ln.i(TAG, "activityName:" + activityName);
                        @SuppressLint("BlockedPrivateApi")
                        int displayId = TaskInfo.class.getDeclaredField("displayId").getInt(taskInfo);
                        Ln.i(TAG, "taskInfo.displayId:" + displayId);
                        if (displayId == DisplayInfo.getMirrorDisplayId() && secondActivity.equals(activityName)) {
                            secondResultStarted = true;
                            if (firstActivityStarted) {
                                break;
                            }
                        } else if (displayId == 0 && firstActivity != null && firstActivity.equals(activityName)) {
                            firstActivityStarted = true;
                            if (secondResultStarted) {
                                break;
                            }
                        }
                    }
                }

                result.add(new Pair<>(firstActivityStarted, secondResultStarted));
            }
            return result;
        } catch (ReflectiveOperationException e1) {
            Ln.e(TAG, "getTasks Could not invoke method", e1);
        }
        return checkActivityReadyByShell(lists);
    }

    private static List<Pair<Boolean, Boolean>> checkStackInfo(@NonNull List<Pair<String, String>> lists, @NonNull List<ActivityManager.StackInfo> stackInfos) {
        List<Pair<Boolean, Boolean>> result = new ArrayList<>();
        for (Pair<String, String> pair : lists) {
            String firstActivity = pair.first;
            String secondActivity = pair.second;
            boolean firstActivityStarted = false;
            boolean secondResultStarted = false;
            if (firstActivity == null) {
                firstActivityStarted = true;
            }

            for (ActivityManager.StackInfo stackInfo : stackInfos) {
                for (String activityName : stackInfo.taskNames) {
                    Ln.i(TAG, "activityName:" + activityName);

                    if (stackInfo.displayId == DisplayInfo.getMirrorDisplayId() && activityName.equals(secondActivity)) {
                        secondResultStarted = true;
                        if (firstActivityStarted) {
                            break;
                        }
                    } else if (stackInfo.displayId == 0 && firstActivity != null && activityName.equals(firstActivity)) {
                        firstActivityStarted = true;
                        if (secondResultStarted) {
                            break;
                        }
                    }
                }

                if (firstActivityStarted && secondResultStarted) {
                    break;
                }
            }

            result.add(new Pair<>(firstActivityStarted, secondResultStarted));
        }
        return result;
    }
}