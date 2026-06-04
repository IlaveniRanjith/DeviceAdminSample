package com.ram.deviceadmin;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String TAG = "AdminActivity";

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    
    private TextView tvLogs;
    private ScrollView logScrollView;

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MyDeviceAdminReceiver.ACTION_LOG_EVENT.equals(intent.getAction())) {
                refreshLogs();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Device Admin");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);

        tvLogs = findViewById(R.id.tvLogs);
        logScrollView = findViewById(R.id.logScrollView);
        
        setupButtons();
        refreshLogs();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(MyDeviceAdminReceiver.ACTION_LOG_EVENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(eventReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(eventReceiver, filter);
        }
        refreshLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(eventReceiver);
    }

    private void refreshLogs() {
        String allLogs = LogRepository.getLogs(this);
        if (allLogs.isEmpty()) {
            tvLogs.setText("No logs available.");
            return;
        }

        // We re-format the text with colors
        String[] lines = allLogs.split("\n");
        tvLogs.setText(""); // Clear current
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            SpannableString spannable = new SpannableString(line + "\n");
            int color = getColor(R.color.log_text_default);
            
            if (line.contains("SUCCESS")) color = getColor(R.color.log_success);
            else if (line.contains("ERROR")) color = getColor(R.color.log_error);
            else if (line.contains("INFO")) color = getColor(R.color.log_info);
            
            spannable.setSpan(new ForegroundColorSpan(color), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvLogs.append(spannable);
        }
        
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void localLog(String message, String type) {
        LogRepository.appendLog(this, message, type);
        refreshLogs();
        Log.d(TAG, message);
    }

    private void setupButtons() {
        findViewById(R.id.btnActivate).setOnClickListener(v -> activateAdmin());
        findViewById(R.id.btnDeactivate).setOnClickListener(v -> deactivateAdmin());
        findViewById(R.id.btnLock).setOnClickListener(v -> lockNow());
        findViewById(R.id.btnSetPasswordPolicy).setOnClickListener(v -> setPasswordPolicy());
        findViewById(R.id.btnWipe).setOnClickListener(v -> wipeDevice());
        findViewById(R.id.btnDisableCamera).setOnClickListener(v -> setCameraDisabled(true));
        findViewById(R.id.btnEnableCamera).setOnClickListener(v -> setCameraDisabled(false));
        findViewById(R.id.btnShowAdminStatus).setOnClickListener(v -> showAdminStatus());
        findViewById(R.id.btnSetTimeout).setOnClickListener(v -> setMaximumTimeToLock());
        findViewById(R.id.btnAdvancedPolicies).setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, AdvancedPoliciesActivity.class));
        });
        
        findViewById(R.id.btnCopyLogs).setOnClickListener(v -> copyLogs());
        findViewById(R.id.btnExportLogs).setOnClickListener(v -> exportLogs());
        findViewById(R.id.btnClearLogs).setOnClickListener(v -> clearLogs());
    }

    private void activateAdmin() {
        if (dpm.isAdminActive(adminComponent)) {
            localLog("Already device admin", "info");
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "This app requires device admin to demonstrate enterprise features.");
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        localLog("Activation requested", "info");
    }

    private void deactivateAdmin() {
        if (!dpm.isAdminActive(adminComponent)) {
            localLog("Not an admin", "info");
            return;
        }
        dpm.removeActiveAdmin(adminComponent);
        localLog("Admin deactivated successfully", "success");
    }

    private void lockNow() {
        if (!dpm.isAdminActive(adminComponent)) {
            localLog("Activate admin first", "info");
            return;
        }
        dpm.lockNow();
        localLog("Device locked", "success");
    }

    private void setPasswordPolicy() {
        if (!dpm.isAdminActive(adminComponent)) {
            localLog("Activate admin first", "info");
            return;
        }
        try {
            dpm.setPasswordQuality(adminComponent, DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
            dpm.setPasswordMinimumLength(adminComponent, 6);
            localLog("Password policy set (alphanumeric, min length 6)", "success");
        } catch (Exception e) {
            localLog("Failed to set password policy: " + e.getMessage(), "error");
        }
    }

    private void wipeDevice() {
        if (!dpm.isAdminActive(adminComponent)) {
            localLog("Activate admin first", "info");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Wipe Device")
                .setMessage("This will factory reset the device. Continue?")
                .setPositiveButton("OK", (dialog, id) -> {
                    try {
                        localLog("Wiping device...", "info");
                        dpm.wipeData(0);
                    } catch (Exception e) {
                        localLog("Wipe failed: " + e.getMessage(), "error");
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss())
                .show();
    }

    private void setCameraDisabled(boolean disabled) {
        if (!dpm.isAdminActive(adminComponent)) {
            localLog("Activate admin first", "info");
            return;
        }
        try {
            dpm.setCameraDisabled(adminComponent, disabled);
            localLog("Camera disabled = " + disabled, "success");
        } catch (Exception e) {
            localLog("Failed to set camera: " + e.getMessage(), "error");
        }
    }

    private void showAdminStatus() {
        boolean active = dpm.isAdminActive(adminComponent);
        localLog("Admin active: " + active, active ? "success" : "info");
    }

    private void setMaximumTimeToLock() {
        if (!dpm.isAdminActive(adminComponent)) {
            localLog("Activate admin first", "info");
            return;
        }
        try {
            long millis = 30 * 1000;
            dpm.setMaximumTimeToLock(adminComponent, millis);
            localLog("Max time to lock set to 30s", "success");
        } catch (Exception e) {
            localLog("Failed to set lock timeout: " + e.getMessage(), "error");
        }
    }

    private void copyLogs() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Device Admin Logs", LogRepository.getLogs(this));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void exportLogs() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, LogRepository.getLogs(this));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Export Logs"));
    }

    private void clearLogs() {
        LogRepository.clearLogs(this);
        refreshLogs();
        localLog("Logs cleared", "info");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (dpm.isAdminActive(adminComponent)) {
                localLog("Admin enabled by user", "success");
            } else {
                localLog("Admin enable canceled by user", "info");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}