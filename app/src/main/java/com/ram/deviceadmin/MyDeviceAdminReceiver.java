package com.ram.deviceadmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    public static final String ACTION_LOG_EVENT = "com.ram.deviceadmin.LOG_EVENT";
    public static final String EXTRA_LOG_MESSAGE = "log_message";
    public static final String EXTRA_LOG_TYPE = "log_type";
    private static final String CHANNEL_ID = "device_admin_channel";

    private void logEvent(Context context, String message, String type) {
        // 1. Persist to SharedPreferences so it survives activity backgrounding
        LogRepository.appendLog(context, message, type);
        
        // 2. Broadcast for immediate UI update if activity is visible
        Intent intent = new Intent(ACTION_LOG_EVENT);
        intent.putExtra(EXTRA_LOG_MESSAGE, message);
        intent.putExtra(EXTRA_LOG_TYPE, type);
        intent.setPackage(context.getPackageName()); // Ensure only our app gets it
        context.sendBroadcast(intent);
        
        Log.d("MyDeviceAdminReceiver", message);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Device Admin Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (PackageInstallationUtils.ACTION_INSTALL_COMPLETE.equals(action)) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                logEvent(context, "Silent installation SUCCESSFUL", "success");
            } else {
                logEvent(context, "Silent installation FAILED: " + msg, "error");
            }
        } else if (PackageInstallationUtils.ACTION_UNINSTALL_COMPLETE.equals(action)) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            String pkg = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                logEvent(context, "Silent uninstallation SUCCESSFUL for " + pkg, "success");
            } else {
                logEvent(context, "Silent uninstallation FAILED for " + pkg, "error");
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "Device admin enabled", Toast.LENGTH_SHORT).show();
        logEvent(context, "Device admin enabled", "success");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device admin disabled", Toast.LENGTH_SHORT).show();
        logEvent(context, "Device admin disabled", "info");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        logEvent(context, "Device password changed", "success");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        logEvent(context, "Password attempt FAILED", "error");
        
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .setContentTitle("Security Alert")
                .setContentText("A failed unlock attempt was detected.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat nm = NotificationManagerCompat.from(context);
            nm.notify(1001, builder.build());
        } catch (SecurityException e) {
            Log.e("MyDeviceAdminReceiver", "Missing notification permission", e);
        }
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        logEvent(context, "Password attempt SUCCESSFUL", "success");
    }
}