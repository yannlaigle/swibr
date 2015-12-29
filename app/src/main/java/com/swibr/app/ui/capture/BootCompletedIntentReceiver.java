package com.swibr.app.ui.capture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by hthetiot on 12/29/15.
 */
public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean captureServiceEnabled = prefs.getBoolean("runCaptureService", false);

            if (captureServiceEnabled) {
                Intent pushIntent = new Intent(context, CaptureService.class);
                context.startService(pushIntent);
            }
        }
    }
}
