package com.overlaywindow.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class DemoApplication extends Application {
    private static String TAG = "DemoApplication";
    public DemoApplication() {
        super();

        Log.i(TAG, "DemoApplication");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        Log.i(TAG, "attachBaseContext");

        Utils.setContext(this);
    }
}
