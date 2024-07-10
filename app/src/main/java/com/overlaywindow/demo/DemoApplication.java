package com.overlaywindow.demo;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import io.github.muntashirakon.adb.PRNGFixes;

public class DemoApplication extends Application {
    private static String TAG = "DemoApplication";
    private static DemoApplication mApp;
    public DemoApplication() {
        super();

        Log.i(TAG, "DemoApplication");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate");
        PRNGFixes.apply();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        Log.i(TAG, "attachBaseContext");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L");
        }

        mApp = this;
    }

    public static DemoApplication getApp() {
        return mApp;
    }
}
