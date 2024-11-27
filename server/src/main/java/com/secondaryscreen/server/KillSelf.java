package com.secondaryscreen.server;

import android.content.Intent;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.ParceledListSlice;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IRemoteCallback;
import android.os.UserHandle;

public class KillSelf {
    private static String TAG = "KillSelf";

    public static void start() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceManager.getPackageManager().registerPackageMonitorCallback(new IRemoteCallback.Stub() {
                @Override
                public void sendResult(Bundle bundle) {
                    Ln.i(TAG, "sendResult bundle:" + bundle);
                    Intent intent = bundle.getParcelable(
                            "android.content.pm.extra.EXTRA_PACKAGE_MONITOR_CALLBACK_RESULT", Intent.class);
                    if (intent != null) {
                        String action = intent.getAction();
                        Uri uri = intent.getData();
                        String packageName = uri != null ? uri.getSchemeSpecificPart() : null;
                        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                            Ln.i(TAG, "onPackageAdded:" + packageName);
                        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                            Ln.i(TAG, "onPackageRemoved:" + packageName);

                            KillSelf.kill(packageName);
                        } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                            Ln.i(TAG, "onPackageChanged:" + packageName);

                            KillSelf.kill(packageName);
                        } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                            Ln.i(TAG, "onPackageDataCleared:" + packageName);
                        } else if (Intent.ACTION_PACKAGE_RESTARTED.equals(action)) {
                            Ln.i(TAG, "onPackageRestarted:" + packageName);
                        } else if (Intent.ACTION_PACKAGES_SUSPENDED.equals(action)) {
                            Ln.i(TAG, "onPackagesSuspended:" + packageName);
                        } else if (Intent.ACTION_PACKAGES_UNSUSPENDED.equals(action)) {
                            Ln.i(TAG, "onPackagesUnsuspended:" + packageName);
                        }
                    }
                }
            }, 0);
        } else {
            ServiceManager.getLauncherApps().addOnAppsChangedListener(new IOnAppsChangedListener.Stub() {
                @Override
                public void onPackageRemoved(UserHandle user, String packageName) {
                    Ln.i(TAG, "onPackageRemoved:" + packageName);

                    KillSelf.kill(packageName);
                }

                @Override
                public void onPackageAdded(UserHandle user, String packageName) {
                    Ln.i(TAG, "onPackageAdded:" + packageName);
                }

                @Override
                public void onPackageChanged(UserHandle user, String packageName) {
                    Ln.i(TAG, "onPackageChanged:" + packageName);

                    // KillSelf.kill(packageName);
                }

                @Override
                public void onPackagesAvailable(UserHandle user, String[] packageNames, boolean replacing) {
                    Ln.i(TAG, "onPackagesAvailable:" + packageNames);
                }

                @Override
                public void onPackagesUnavailable(UserHandle user, String[] packageNames, boolean replacing) {
                    Ln.i(TAG, "onPackagesUnavailable:" + packageNames);
                }

                @Override
                public void onPackagesSuspended(UserHandle user, String[] packageNames,
                                                Bundle launcherExtras) {
                    Ln.i(TAG, "onPackagesSuspended:" + packageNames);
                }

                @Override
                public void onPackagesUnsuspended(UserHandle user, String[] packageNames) {
                    Ln.i(TAG, "onPackagesUnsuspended:" + packageNames);
                }

                @Override
                public void onShortcutChanged(UserHandle user, String packageName, ParceledListSlice shortcuts) {
                    Ln.i(TAG, "onShortcutChanged:" + packageName);
                }

                @Override
                public void onPackageLoadingProgressChanged(UserHandle user, String packageName, float progress) {
                    Ln.i(TAG, "onPackageLoadingProgressChanged:" + packageName);
                }
            });
        }

    }

    private static void kill(String packageName) {
        if ("com.secondaryscreen.app".equals(packageName)) {
            Ln.i(TAG, "Kill Self...");
            System.exit(0);
        }
    }
}