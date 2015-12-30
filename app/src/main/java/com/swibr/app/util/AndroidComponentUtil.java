package com.swibr.app.util;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;

public final class AndroidComponentUtil {

    private static final String TAG = AndroidComponentUtil.class.getName();

    public static void toggleComponent(Context context, Class componentClass, boolean enable) {
        ComponentName componentName = new ComponentName(context, componentClass);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(componentName,
                enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static boolean isServiceRunning(Context context, Class serviceClass) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRunningOnEmulator() {
        return "google_sdk".equals(Build.PRODUCT) || Build.FINGERPRINT.startsWith("generic");
    }

    public void publishImageFile(Context context, File imageFile) {
        // Tell the framework, so the image will be in the gallery
        MediaScannerConnection.scanFile(context,
            new String[]{imageFile.getAbsolutePath()},
            new String[]{"image/png"},
            new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i(TAG, "Scanned " + path + ":");
                    Log.i(TAG, "-> uri=" + uri);
                }
            });
    }

    public static boolean isUsageStatsEnabled(Context context) {

        try {

            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
            }
            return mode == AppOpsManager.MODE_ALLOWED;

        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }

        return true;
    }
}
