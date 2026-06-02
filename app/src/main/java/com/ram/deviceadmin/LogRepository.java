package com.ram.deviceadmin;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogRepository {
    private static final String PREF_NAME = "device_admin_logs";
    private static final String KEY_LOGS = "full_logs";

    public static void appendLog(Context context, String message, String type) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentLogs = prefs.getString(KEY_LOGS, "");
        
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String formattedMessage = "[" + timestamp + "] " + type.toUpperCase() + ": " + message + "\n";
        
        // Use commit() to ensure it's written before broadcast receiver finishes
        prefs.edit().putString(KEY_LOGS, currentLogs + formattedMessage).commit();
    }

    public static String getLogs(Context context) {
        if (context == null) return "";
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LOGS, "");
    }

    public static void clearLogs(Context context) {
        if (context == null) return;
        context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }
}