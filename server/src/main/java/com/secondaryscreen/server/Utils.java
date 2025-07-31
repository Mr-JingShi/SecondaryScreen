package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

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
    static int DISPLAY_CHANNEL_PORT = 8404;
    private static ScheduledExecutorService mExecutor  = Executors.newSingleThreadScheduledExecutor();

    static void startActivity(@NonNull String activity, int displayId) {
        StringBuilder sb = new StringBuilder();

        sb.append("am start -n ");
        sb.append(activity);
        sb.append(" -a ");
        sb.append(Intent.ACTION_MAIN);
        sb.append(" -c ");
        sb.append(Intent.CATEGORY_LAUNCHER);
        sb.append(" -f ");
        sb.append(Intent.FLAG_ACTIVITY_NEW_TASK);
        sb.append(" --display ");
        sb.append(displayId);

        String text = sb.toString();
        Ln.i(TAG, "startActivity:" + text);
        try {
            Shell.exec("sh", "-c", text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void moveTaskToDisplay(int taskId, int displayId) {
        Ln.i(TAG, "moveTaskToDisplay taskId:" + taskId + " displayId:" + displayId);

        StringBuilder sb = new StringBuilder();

        sb.append("am display move-stack ");
        sb.append(taskId);
        sb.append(" ");
        sb.append(displayId);

        String text = sb.toString();
        Ln.i(TAG, "moveTaskToDisplay:" + text);
        try {
            Shell.exec("sh", "-c", text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void startActivity(@NonNull String packageName, @NonNull String className, int displayId) {
        Ln.i(TAG, "startActivity packageName:" + packageName + " className:" + className + " displayId:" + displayId);
        if (ServiceManager.getDisplayManager().isActive(displayId)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(packageName);
            intent.setClassName(packageName, className);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);

            int result = ServiceManager.getActivityManager().startActivity(intent, options.toBundle());
            Ln.i(TAG, "startActivity result:" + result);
            if (result != 0) {
                Ln.e(TAG, "Could not start activity by ActivityManager");
                String activityName = packageName + "/" + className;
                Utils.startActivity(activityName, displayId);
            }
        }
    }

    public static int byte4ToInt(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    static void schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        mExecutor.schedule(runnable, time, timeUnit);
    }

    static void shutdown() {
        mExecutor.shutdown();
    }

    @RequiresApi(api = 29)
    static void checkActivityReady(@NonNull String packageName, int displayId) {
        try {
            List<ActivityManager.StackInfo> stackInfos = ServiceManager.getActivityManager().getAllTaskInfos();
            checkStackInfo(packageName, displayId, stackInfos);
            return;
        } catch (ReflectiveOperationException e) {
            Ln.e(TAG, "getAllTaskInfos Could not invoke method", e);
        }

        try {
            List<TaskInfo> taskInfos = ServiceManager.getActivityManager().getTasks(9999);
            for (TaskInfo taskInfo : taskInfos) {
                if (taskInfo.baseIntent.getComponent() != null) {
                    String taskInfoPackageName = taskInfo.baseIntent.getComponent().getPackageName();
                    String taskInfoClassName = taskInfo.baseIntent.getComponent().getClassName();
                    String taskInfoActivityName = taskInfoPackageName + "/" + taskInfoClassName;
                    @SuppressLint("BlockedPrivateApi")
                    int taskInfoDisplayId = TaskInfo.class.getDeclaredField("displayId").getInt(taskInfo);
                    if (taskInfoPackageName.contains(packageName) && taskInfoDisplayId != displayId) {
                        Ln.i(TAG, "activityName:" + taskInfoActivityName + " displayId:" + taskInfoDisplayId + " taskId:" + taskInfo.taskId);
                        moveTaskToDisplay(taskInfo.taskId, displayId);
                    }
                }
            }
            return;
        } catch (ReflectiveOperationException e1) {
            Ln.e(TAG, "getTasks Could not invoke method", e1);
        }
        checkActivityReadyByShell(packageName, displayId);
    }

    static void checkActivityReadyByShell(@NonNull String packageName, int displayId) {
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
                        int index1 = line.indexOf("taskId=");
                        int index2 = line.indexOf(":", index1 + 7);
                        stackInfo.taskId = Integer.parseInt(line.substring(index1 + 7, index2));
                        int index3 = line.indexOf(" ", index2 + 1);
                        int index4 = line.indexOf(" ", index3 + 1);
                        String activityName = line.substring(index3 + 1, index4);
                        stackInfo.taskNames.add(activityName);
                    }
                }
            }
            checkStackInfo(packageName, displayId, stackInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkStackInfo(@NonNull String packageName, int displayId, @NonNull List<ActivityManager.StackInfo> stackInfos) {
        for (ActivityManager.StackInfo stackInfo : stackInfos) {
            for (String activityName : stackInfo.taskNames) {
                if (activityName.contains(packageName) && stackInfo.displayId != displayId) {
                    Ln.i(TAG, "activityName:" + activityName + " displayId:" + stackInfo.displayId + " taskId:" + stackInfo.taskId);
                    moveTaskToDisplay(stackInfo.taskId, displayId);
                }
            }
        }
    }
}