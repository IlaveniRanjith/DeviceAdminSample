package com.ram.deviceadmin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RemoteConnectionService extends Service {
    private static final String CHANNEL_ID = "remote_connection_channel";
    private static final int NOTIFICATION_ID = 1002;
    private RemoteConnectionManager remoteManager;

    @Override
    public void onCreate() {
        super.onCreate();
        remoteManager = RemoteConnectionManager.getInstance(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification("Connecting to server..."));

        String ip = remoteManager.getServerIp();
        String port = remoteManager.getServerPort();

        if (!ip.isEmpty()) {
            remoteManager.connect(ip, port, new RemoteConnectionManager.ConnectionCallback() {
                @Override
                public void onConnected() {
                    updateNotification("Connected to Remote Server");
                    broadcastStatus(true);
                }

                @Override
                public void onDisconnected() {
                    updateNotification("Disconnected from server");
                    broadcastStatus(false);
                }

                @Override
                public void onError(String error) {
                    updateNotification("Connection Error: " + error);
                    broadcastStatus(false);
                }
            });
        }

        return START_STICKY;
    }

    private void broadcastStatus(boolean connected) {
        Intent intent = new Intent(MyDeviceAdminReceiver.ACTION_LOG_EVENT);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Remote Management Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Device Admin Remote")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String content) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(content));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        remoteManager.disconnect();
        super.onDestroy();
    }
}
