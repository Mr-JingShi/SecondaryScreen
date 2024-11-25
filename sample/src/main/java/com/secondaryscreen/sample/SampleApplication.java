package com.secondaryscreen.sample;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class SampleApplication extends Application {
    private static String TAG = "SampleApplication";

    public SampleApplication() {
        super();
        Log.i(TAG, "DemoApplication");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "SampleApplication onCreate");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.i(TAG, "attachBaseContext");
    }
}
