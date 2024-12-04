package com.secondaryscreen.server;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
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
                ArrayList<Pair<String, String>> lists = new ArrayList<>();
                lists.add(new Pair<>(null, activityName));
                List<Pair<Boolean, Boolean>> result = Utils.checkActivityReady(lists);
                Ln.i(TAG, "SecondaryDisplayLauncher result.get(0).first:" + result.get(0).first + ", result.get(0).second:" + result.get(0).second);
                if (!result.get(0).second) {
                    Ln.i(TAG, "Secondary launcher activity have not start, start secondary launcher activity");
                    /**
                     * Virtual display flag: Indicates that the display should support system decorations. Virtual
                     * displays without this flag shouldn't show home, IME or any other system decorations.
                     * <p>This flag doesn't work without {@link #VIRTUAL_DISPLAY_FLAG_TRUSTED}</p>
                     *
                     * @see #createVirtualDisplay
                     * @see #VIRTUAL_DISPLAY_FLAG_TRUSTED
                     * @hide
                     */
                    // TODO (b/114338689): Remove the flag and use IWindowManager#setShouldShowSystemDecors
                    // public static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;

                    /**
                     * Virtual display flags: Indicates that the display is trusted to show system decorations and
                     * receive inputs without users' touch.
                     *
                     * @see #createVirtualDisplay
                     * @see #VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS
                     * @hide
                     */
                    // public static final int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;

                    /** remark
                     *
                     * Android 10 无VIRTUAL_DISPLAY_FLAG_TRUSTED标识，server端创建的virtualdisplay可显示home，IME或任何系统装饰
                     *
                     * Android 11 ～ 12 开发者选项-模拟辅助显示设备创建的virtualdisplay携带FLAG_TRUSTED标识，因此可以显示home，IME或任何系统装饰
                     * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 3", displayGroupId 0, FLAG_PRESENTATION, FLAG_TRUSTED, real 1920 x 1080, largest app 1920 x 1080, smallest app 1920 x 1080, appVsyncOff 0, presDeadline 33333332, mode 2, defaultMode 2, modes [{id=2, width=1920, height=1080, fps=60.000004, alternativeRefreshRates=[]}], hdrCapabilities null, userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "****", app 1920 x 1080, density 320 (320.0 x 320.0) dpi, layerStack 3, colorMode 0, supportedColorModes [0], deviceProductInfo null, removeMode 0, refreshRateOverride 0.0, brightnessMinimum 0.0, brightnessMaximum 0.0, brightnessDefault 0.0}
                     * Task任务中mActivityType=home、displayId=3，am stack list抓到的详情如下：
                     * RootTask id=174 bounds=[0,0][1080,1920] displayId=3 userId=0
                     *  configuration={1.0 ?mcc?mnc [zh_CN_#Hans] ldltr sw540dp w540dp h913dp 320dpi lrg long port -touch -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1080, 1920) mAppBounds=Rect(0, 0 - 1080, 1826) mMaxBounds=Rect(0, 0 - 1080, 1920) mWindowingMode=fullscreen mDisplayWindowingMode=freeform mActivityType=home mAlwaysOnTop=undefined mRotation=ROTATION_90} suim:1 extflag:8 s.32 fontWeightAdjustment=0}
                     *   taskId=175: com.huawei.android.launcher/com.huawei.android.launcher.secondarydisplay.SecondaryDisplayLauncher bounds=[0,0][1080,1920] userId=0 visible=true topActivity=ComponentInfo{com.huawei.android.launcher/com.huawei.android.launcher.unihome.UniHomeLauncher}
                     *
                     * Android 11 ～ 12 server端创建的virtualdisplay因为权限问题无法添加VIRTUAL_DISPLAY_FLAG_TRUSTED标识
                     * 导致server端创建的virtualdisplay无法显示home，IME或任何系统装饰
                     * launcherIntent添加CATEGORY_SECONDARY_HOME后，SecondaryDisplayLauncher会追加到内置屏幕（vdisplayId=0）
                     * mBaseDisplayInfo=DisplayInfo{"secondaryscreen", displayId 4", displayGroupId 0, FLAG_PRIVATE, FLAG_PRESENTATION, real 1200 x 2000, largest app 1200 x 2000, smallest app 1200 x 2000, appVsyncOff 0, presDeadline 16666666, mode 3, defaultMode 3, modes [{id=3, width=1200, height=2000, fps=60.0, alternativeRefreshRates=[]}], hdrCapabilities null, userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, type VIRTUAL, uniqueId "****", app 1200 x 2000, density 320 (320.0 x 320.0) dpi, layerStack 4, colorMode 0, supportedColorModes [0], deviceProductInfo null, owner com.android.shell (uid 2000), removeMode 1, refreshRateOverride 0.0, brightnessMinimum 0.0, brightnessMaximum 0.0, brightnessDefault 0.0}
                     * Task任务中mActivityType=home、displayId=0，am stack list抓到的详情如下：
                     * RootTask id=1 bounds=[0,0][1200,2000] displayId=0 userId=0
                     *  configuration={1.0 ?mcc?mnc [zh_CN_#Hans] ldltr sw600dp w600dp h976dp 320dpi lrg port finger -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1200, 2000) mAppBounds=Rect(0, 0 - 1200, 2000) mMaxBounds=Rect(0, 0 - 1200, 2000) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=home mAlwaysOnTop=undefined mRotation=ROTATION_0} suim:1 extflag:8 s.306 fontWeightAdjustment=0}
                     *   taskId=166: com.huawei.android.launcher/com.huawei.android.launcher.unihome.UniHomeLauncher bounds=[0,0][1200,2000] userId=0 visible=true topActivity=ComponentInfo{com.huawei.android.launcher/com.huawei.android.launcher.secondarydisplay.SecondaryDisplayLauncher}
                     *
                     * launcherIntent不添加CATEGORY_SECONDARY_HOME标识时，SecondaryDisplayLauncher会追加到指定屏幕
                     * 但是SecondaryDisplayLauncher是作为一个新的APP启动的，在最近活动的APP列表中可以看到，并且可以手动关闭
                     * 添加FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS标识后，可以隐藏将SecondaryDisplayLauncher在最近活动列表中隐藏，防止用户手动关闭。
                     * Task任务中mActivityType=standard、displayId=8，am stack list抓到的详情如下：
                     * RootTask id=172 bounds=[0,0][1200,2000] displayId=8 userId=0
                     *  configuration={1.0 ?mcc?mnc [zh_CN_#Hans] ldltr sw600dp w600dp h1000dp 320dpi lrg port -touch -keyb/v/h -nav/h winConfig={ mBounds=Rect(0, 0 - 1200, 2000) mAppBounds=Rect(0, 0 - 1200, 2000) mMaxBounds=Rect(0, 0 - 1200, 2000) mWindowingMode=fullscreen mDisplayWindowingMode=fullscreen mActivityType=standard mAlwaysOnTop=undefined mRotation=ROTATION_0} suim:1 extflag:8 s.306 fontWeightAdjustment=0}
                     *   taskId=172: com.huawei.android.launcher/com.huawei.android.launcher.secondarydisplay.SecondaryDisplayLauncher bounds=[0,0][1200,2000] userId=0 visible=true topActivity=ComponentInfo{com.huawei.android.launcher/com.huawei.android.launcher.secondarydisplay.SecondaryDisplayLauncher}
                     */
                    Intent launcherIntent = new Intent();
                    launcherIntent.setAction(Intent.ACTION_MAIN);
                    if (false) {
                        launcherIntent.addCategory(Intent.CATEGORY_SECONDARY_HOME);
                        launcherIntent.setPackage(packageName);
                    } else {
                        launcherIntent.setClassName(packageName, className);
                    }
                    launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    /**
                     * FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS 具有这个标记的 Activity 不会出现在历史 Activity 的列表中，
                     * 在某些情况下我们不希望用户通过历史列表回到我们的 Activity 的时候这个标记比较有用。
                     * 它等同于在 XML 中指定 Activity 的属性 android:excludeFromRecents="true"。
                     * 如果您想在“最近使用的应用”屏幕中保留某个任务，可传递Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS标记。
                     *
                     * autoRemoveFromRecents 和 excludeFromRecents
                     *
                     * android:autoRemoveFromRecents
                     * android:autoRemoveFromRecents 是在任务栈中的最后一个 Activity 完成之前，由具有此属性的 Activity 启动的任务栈是否保留在多任务切换页面中。即 autoRemoveFromRecents 指定了当 Activity 被系统回收时，是否保留在多任务切换页面中。默认值为 false。
                     * 当设置为 true 时: 当 Activity 被系统回收时，从最近使用的多任务切换页中移除该 Activity 所在的任务栈；当设置为 false 时: 当 Activity 被系统回收时，不从最近使用的多任务切换页中移除该 Activity 所在的任务栈。
                     * 这个属性主要用于:
                     * 1）一些临时 Activity，当它们被销毁后，不希望它们出现在多任务切换页中，可以设置为 true；
                     * 2）一些没有重要数据的 Activity，如果设置为 true，当内存不足被系统回收后，由于它已经从多任务切换页移除，用户不太可能再去恢复它，及时移除有利于内存回收；
                     * 3）一些包含敏感数据的 Activity，为了安全考虑，不希望它出现在多任务切换页中,可以设置为 true。
                     * 所以，总体来说，这个属性主要是出于内存管理和安全考虑，控制 Activity 在被系统回收后是否从多任务切换页中移除。
                     *
                     * android:excludeFromRecents
                     * android:excludeFromRecents 也是一个 Activity 属性，它指定了是否从多任务切换页中排除该 Activity 所在的任务栈。默认值为 false。
                     * 当设置为 true 时：该 Activity 所在的任务栈不会出现在多任务切换页中；当设置为 false 时：不从多任务切换页中排除该 Activity 所在的任务栈。
                     * 这个属性与 android:autoRemoveFromRecents 很像，它们的区别是:
                     * android:autoRemoveFromRecents 是当 Activity 被系统回收时，所在栈是否从多任务切换页中移除；
                     * android:excludeFromRecents 是 Activity 所在的栈从一开始就不会出现在任务列表中。
                     * */
                    launcherIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    ActivityOptions options = ActivityOptions.makeBasic();
                    options.setLaunchDisplayId(DisplayInfo.getMirrorDisplayId());

                    int ret = ServiceManager.getActivityManager().startActivity(launcherIntent, options.toBundle());
                    Ln.i(TAG, "Start secondary launcher activity ret:" + ret);
                    if (ret < 0) {
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
