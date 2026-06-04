package com.ram.deviceadmin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PackageInstallationUtils {

    public static final String ACTION_INSTALL_COMPLETE = "com.ram.deviceadmin.INSTALL_COMPLETE";
    public static final String ACTION_UNINSTALL_COMPLETE = "com.ram.deviceadmin.UNINSTALL_COMPLETE";

    public static boolean installPackage(Context context, InputStream in, String packageName) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        if (packageName != null) {
            params.setAppPackageName(packageName);
        }
        
        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        OutputStream out = session.openWrite("MDM_Installer", 0, -1);
        byte[] buffer = new byte[65536];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        session.fsync(out);
        in.close();
        out.close();

        session.commit(createInstallIntentSender(context, sessionId));
        return true;
    }

    public static void uninstallPackage(Context context, String packageName) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        packageInstaller.uninstall(packageName, createUninstallIntentSender(context, packageName));
    }

    private static IntentSender createInstallIntentSender(Context context, int sessionId) {
        Intent intent = new Intent(ACTION_INSTALL_COMPLETE);
        intent.setPackage(context.getPackageName());
        
        // Android 12+ (API 31+) requires PendingIntents for PackageInstaller to be MUTABLE
        // so the system can inject status results into them.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags);
        return pendingIntent.getIntentSender();
    }

    private static IntentSender createUninstallIntentSender(Context context, String packageName) {
        Intent intent = new Intent(ACTION_UNINSTALL_COMPLETE);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
        intent.setPackage(context.getPackageName());
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return pendingIntent.getIntentSender();
    }
}