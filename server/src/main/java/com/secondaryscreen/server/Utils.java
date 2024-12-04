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

    static boolean activityRunning(@NonNull String activity) {
        try {
            String cmd = "am stack list | grep " + activity + " | wc -l";
            String result = Shell.execReadOutput("sh", "-c", cmd);
            int count = Integer.parseInt(result.trim());
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
            List<ActivityManager.StackInfo> StackInfos = ServiceManager.getActivityManager().getAllTaskInfos();
            Ln.i(TAG, "StackInfos size:" + StackInfos.size());

            List<Pair<Boolean, Boolean>> result = new ArrayList<>();
            for (Pair<String, String> pair : lists) {
                String firstActivity = pair.first;
                String secondActivity = pair.second;
                boolean firstActivityStarted = false;
                boolean secondResultStarted = false;
                if (firstActivity == null) {
                    firstActivityStarted = true;
                }

                for (ActivityManager.StackInfo stackInfo : StackInfos) {
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
        } catch (ReflectiveOperationException e) {
            Ln.e(TAG, "getAllTaskInfos Could not invoke method", e);

            try {
                List<TaskInfo> TaskInfos = ServiceManager.getActivityManager().getTasks(9999);
                Ln.i(TAG, "TaskInfos size:" + TaskInfos.size());

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

                    for (TaskInfo taskInfo : TaskInfos) {
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
        }

        return null;
        //return !Utils.activityRunning(secondActivity) && Utils.activityRunning(firstActivity);
    }
}