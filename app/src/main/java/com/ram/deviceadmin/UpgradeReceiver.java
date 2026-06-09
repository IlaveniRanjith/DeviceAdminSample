package com.ram.deviceadmin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class UpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Log.d("UpgradeReceiver", "App upgraded successfully!");
            
            // 1. Log the upgrade event to the dashboard
            LogRepository.appendLog(context, "APPLICATION SELF-UPGRADE COMPLETED", "success");
            
            // 2. Restart the Remote Management Service
            Intent serviceIntent = new Intent(context, RemoteConnectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // 3. Notify any visible activities (if any)
            Intent logIntent = new Intent(MyDeviceAdminReceiver.ACTION_LOG_EVENT);
            logIntent.setPackage(context.getPackageName());
            context.sendBroadcast(logIntent);
        }
    }
}
