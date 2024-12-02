package com.secondaryscreen.server;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.concurrent.TimeUnit;

public final class SecondaryDisplayLauncher {
    private static String TAG = "SecondaryDisplayLauncher";
    @RequiresApi(api = Build.VERSION_CODES.Q)
    static void start() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = (ResolveInfo) ServiceManager.getPackageManager().resolveActivity(
                    /* intent */ intent,
                    /* flags */ android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                    /* userId */ /* UserHandle.USER_SYSTEM */ 0);

            Ln.i(TAG, "launcher packageName:" + resolveInfo.activityInfo.packageName);
            String packageName = resolveInfo.activityInfo.packageName;

            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_SECONDARY_HOME);
            intent.setPackage(resolveInfo.activityInfo.packageName);
            resolveInfo = (ResolveInfo) ServiceManager.getPackageManager().resolveActivity(
                    /* intent */ intent,
                    /* flags */ android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                    /* userId */ /* UserHandle.USER_SYSTEM */ 0);

            Ln.i(TAG, "secondary launcher className:" + resolveInfo.activityInfo.name);

            String className = resolveInfo.activityInfo.name;

            String activityName = packageName + "/" + className;
            Ln.i(TAG, "secondary launcher activityName:" + activityName);

            Utils.schedule(() -> {
                if (Utils.isActivityReady(null, activityName)) {
                    Ln.i(TAG, "Secondary launcher activity have not start, start secondary launcher activity");
                    Intent launcherIntent = new Intent();
                    launcherIntent.setClassName(packageName, className);
                    ActivityOptions options = ActivityOptions.makeBasic();
                    options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());

                    int result = ServiceManager.getActivityManager().startActivity(launcherIntent, options.toBundle());
                    Ln.i(TAG, "Start secondary launcher activity result:" + result);
                    if (result < 0) {
                        Ln.e(TAG, "Could not start secondary launcher activity by ActivityManager");
                        Utils.startActivity(activityName, DisplayInfo.getMirrorDisplayId());
                    }
                }
            }, 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Ln.w(TAG, "SecondaryDisplayLauncher failed", e);
        }
    }
}
