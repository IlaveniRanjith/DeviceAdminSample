package com.ram.deviceadmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PolicyManagementFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_PICK_APK = 1001;
    private DevicePolicyManagerGateway gateway;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.device_policies, rootKey);
        gateway = new DevicePolicyManagerGateway(requireContext());

        setupPackageManagement();
        setupUserRestrictions();
        setupHardwareMedia();
        setupSecurityLockScreen();
        setupAdvancedMDM();
        setupWifiManagement();
        setupSystemSettings();
    }

    private void setupPackageManagement() {
        findPreference("silent_install").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.android.package-archive");
            startActivityForResult(intent, REQUEST_CODE_PICK_APK);
            return true;
        });

        findPreference("silent_uninstall_list").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Silent Uninstall", 
                app -> (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                (pkg) -> {
                    PackageInstallationUtils.uninstallPackage(requireContext(), pkg);
                    log("Silent uninstall requested for: " + pkg, "info");
                });
            return true;
        });

        findPreference("block_uninstallation_list").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Block Uninstallation", 
                app -> true,
                (pkg) -> gateway.setUninstallBlocked(pkg, true,
                    msg -> log(msg, "success"),
                    e -> log("Block uninstall failed: " + e.getMessage(), "error")
                ));
            return true;
        });

        findPreference("hide_apps_list").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Hide App", 
                app -> {
                    try {
                        return !gateway.getDpm().isApplicationHidden(gateway.getAdmin(), app.packageName);
                    } catch (SecurityException e) {
                        return false;
                    }
                },
                (pkg) -> gateway.setApplicationHidden(pkg, true,
                    msg -> log(msg, "success"),
                    e -> log("Hide app failed: " + e.getMessage(), "error")
                ));
            return true;
        });

        findPreference("unhide_apps_list").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Unhide App", 
                app -> {
                    try {
                        return gateway.getDpm().isApplicationHidden(gateway.getAdmin(), app.packageName);
                    } catch (SecurityException e) {
                        return false;
                    }
                },
                (pkg) -> gateway.setApplicationHidden(pkg, false,
                    msg -> log(msg, "success"),
                    e -> log("Unhide app failed: " + e.getMessage(), "error")
                ));
            return true;
        });

        findPreference("suspend_apps_list").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Suspend App", 
                app -> true,
                (pkg) -> gateway.setPackagesSuspended(new String[]{pkg}, true,
                    failed -> {
                        if (failed.length == 0) log("App " + pkg + " suspended", "success");
                        else log("Failed to suspend " + pkg, "error");
                    },
                    e -> log("Suspend app failed: " + e.getMessage(), "error")
                ));
            return true;
        });

        findPreference("clear_app_data_list").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Clear App Data", 
                app -> true,
                (pkg) -> {
                    gateway.getDpm().clearApplicationUserData(gateway.getAdmin(), pkg, requireContext().getMainExecutor(), (packageName, success) -> {
                        if (success) log("Data cleared for " + pkg, "success");
                        else log("Failed to clear data for " + pkg, "error");
                    });
                });
            return true;
        });

        findPreference("install_existing_package").setOnPreferenceChangeListener((preference, newValue) -> {
            String pkg = (String) newValue;
            try {
                boolean result = gateway.getDpm().installExistingPackage(gateway.getAdmin(), pkg);
                if (result) log("Package installed: " + pkg, "success");
                else log("Failed to install existing package: " + pkg, "error");
            } catch (Exception e) {
                log("Install error: " + e.getMessage(), "error");
            }
            return true;
        });
    }

    private void setupUserRestrictions() {
        bindRestriction("disallow_factory_reset", UserManager.DISALLOW_FACTORY_RESET);
        bindRestriction("disallow_add_user", UserManager.DISALLOW_ADD_USER);
        bindRestriction("disallow_mount_physical_media", UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);
        bindRestriction("disallow_config_wifi", UserManager.DISALLOW_CONFIG_WIFI);
        bindRestriction("disallow_install_unknown_sources", UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
        bindRestriction("disallow_debugging_features", UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    private void setupHardwareMedia() {
        bindHardwarePolicy("disable_camera", (enabled) -> gateway.setCameraDisabled(enabled, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")));
        bindHardwarePolicy("disable_screen_capture", (enabled) -> gateway.setScreenCaptureDisabled(enabled, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")));
        bindHardwarePolicy("mute_audio", (enabled) -> gateway.setMasterVolumeMuted(enabled, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")));
    }

    private void setupSecurityLockScreen() {
        bindHardwarePolicy("disable_keyguard", (enabled) -> gateway.setKeyguardDisabled(enabled, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")));

        EditTextPreference lockInfoPref = findPreference("set_lockscreen_info");
        lockInfoPref.setOnPreferenceChangeListener((preference, newValue) -> {
            gateway.getDpm().setDeviceOwnerLockScreenInfo(gateway.getAdmin(), (String) newValue);
            log("Lock screen info set", "success");
            return true;
        });

        findPreference("set_permission_policy").setOnPreferenceClickListener(preference -> {
            String[] options = {"Prompt", "Auto Grant", "Auto Deny"};
            new AlertDialog.Builder(requireContext())
                .setTitle("Default Permission Policy")
                .setItems(options, (dialog, which) -> {
                    gateway.setPermissionPolicy(which, msg -> log(msg, "success"), e -> log(e.getMessage(), "error"));
                }).show();
            return true;
        });

        findPreference("manage_app_permissions").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Pick App for Permission", 
                app -> true,
                this::showPermissionManagementDialog);
            return true;
        });
    }

    private void showPermissionManagementDialog(String pkg) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        final EditText input = new EditText(requireContext());
        input.setHint("e.g., android.permission.CAMERA");
        layout.addView(input);

        new AlertDialog.Builder(requireContext())
            .setTitle("Permission for " + pkg)
            .setView(layout)
            .setPositiveButton("Grant", (dialog, which) -> gateway.setPermissionGrantState(pkg, input.getText().toString(), DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")))
            .setNegativeButton("Deny", (dialog, which) -> gateway.setPermissionGrantState(pkg, input.getText().toString(), DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")))
            .setNeutralButton("Default", (dialog, which) -> gateway.setPermissionGrantState(pkg, input.getText().toString(), DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT, msg -> log(msg, "success"), e -> log(e.getMessage(), "error")))
            .show();
    }

    private void setupAdvancedMDM() {
        findPreference("set_delegated_scopes").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Delegate Scopes", 
                app -> true,
                (pkg) -> {
                    List<String> scopes = new ArrayList<>();
                    scopes.add(DevicePolicyManager.DELEGATION_CERT_INSTALL);
                    scopes.add(DevicePolicyManager.DELEGATION_APP_RESTRICTIONS);
                    scopes.add(DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL);
                    scopes.add(DevicePolicyManager.DELEGATION_PERMISSION_GRANT);
                    gateway.setDelegatedScopes(pkg, scopes, msg -> log(msg, "success"), e -> log(e.getMessage(), "error"));
                });
            return true;
        });

        findPreference("set_accessibility_services").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Restrict Accessibility", 
                app -> true,
                (pkg) -> gateway.setPermittedAccessibilityServices(Collections.singletonList(pkg), msg -> log(msg, "success"), e -> log(e.getMessage(), "error")));
            return true;
        });

        findPreference("set_input_methods").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Restrict Input Methods", 
                app -> true,
                (pkg) -> gateway.setPermittedInputMethods(Collections.singletonList(pkg), msg -> log(msg, "success"), e -> log(e.getMessage(), "error")));
            return true;
        });

        findPreference("set_notification_listeners").setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog("Restrict Notifications", 
                app -> true,
                (pkg) -> {
                    try {
                        gateway.getDpm().setPermittedCrossProfileNotificationListeners(gateway.getAdmin(), Collections.singletonList(pkg));
                        log("Notification listeners restricted to " + pkg, "success");
                    } catch (Exception e) {
                        log("Failed to restrict notification listeners: " + e.getMessage(), "error");
                    }
                });
            return true;
        });
    }

    private void setupWifiManagement() {
        findPreference("set_wifi_security_level").setOnPreferenceChangeListener((preference, newValue) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int level = Integer.parseInt((String) newValue);
                gateway.getDpm().setMinimumRequiredWifiSecurityLevel(level);
                log("Min WiFi security set to " + newValue, "success");
            } else {
                log("WiFi security level restriction requires Android 13+", "error");
            }
            return true;
        });

        findPreference("set_wifi_ssid_restriction").setOnPreferenceChangeListener((preference, newValue) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                String ssids = (String) newValue;
                if (ssids.isEmpty()) {
                    gateway.getDpm().setWifiSsidPolicy(null);
                    log("SSID restriction removed", "success");
                } else {
                    java.util.Set<android.net.wifi.WifiSsid> ssidSet = new java.util.HashSet<>();
                    for (String s : ssids.split(",")) {
                        ssidSet.add(android.net.wifi.WifiSsid.fromBytes(s.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    }
                    gateway.getDpm().setWifiSsidPolicy(new android.app.admin.WifiSsidPolicy(android.app.admin.WifiSsidPolicy.WIFI_SSID_POLICY_TYPE_ALLOWLIST, ssidSet));
                    log("SSID allowlist set: " + ssids, "success");
                }
            } else {
                log("SSID restriction requires Android 13+", "error");
            }
            return true;
        });
    }

    private void setupSystemSettings() {
        EditTextPreference brightnessPref = findPreference("set_screen_brightness");
        brightnessPref.setOnPreferenceChangeListener((preference, newValue) -> {
            gateway.setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, (String) newValue, msg -> log(msg, "success"), e -> log(e.getMessage(), "error"));
            return true;
        });

        EditTextPreference timeoutPref = findPreference("set_screen_off_timeout");
        timeoutPref.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                long millis = Long.parseLong((String) newValue) * 1000;
                gateway.setSystemSetting(Settings.System.SCREEN_OFF_TIMEOUT, String.valueOf(millis), msg -> log(msg, "success"), e -> log(e.getMessage(), "error"));
            } catch (Exception e) {
                log("Invalid timeout value", "error");
            }
            return true;
        });
    }

    private void bindRestriction(String prefKey, String restriction) {
        SwitchPreferenceCompat pref = findPreference(prefKey);
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            gateway.setUserRestriction(restriction, (Boolean) newValue, msg -> log(msg, "success"), e -> log(e.getMessage(), "error"));
            return true;
        });
    }

    private void bindHardwarePolicy(String prefKey, java.util.function.Consumer<Boolean> action) {
        SwitchPreferenceCompat pref = findPreference(prefKey);
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            action.accept((Boolean) newValue);
            return true;
        });
    }

    private void showAppSelectionDialog(String title, Predicate<ApplicationInfo> filter, java.util.function.Consumer<String> onSelected) {
        PackageManager pm = requireContext().getPackageManager();
        // Use MATCH_UNINSTALLED_PACKAGES to ensure hidden apps are included in the results
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES);
        List<ApplicationInfo> filteredApps = new ArrayList<>();
        
        for (ApplicationInfo app : allApps) {
            try {
                if (filter.test(app)) {
                    filteredApps.add(app);
                }
            } catch (Exception e) {
                // Ignore apps that trigger security exceptions during filtering
            }
        }

        Collections.sort(filteredApps, (a, b) -> pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        String[] labels = new String[filteredApps.size()];
        String[] packageNames = new String[filteredApps.size()];
        for (int i = 0; i < filteredApps.size(); i++) {
            labels[i] = pm.getApplicationLabel(filteredApps.get(i)).toString() + " (" + filteredApps.get(i).packageName + ")";
            packageNames[i] = filteredApps.get(i).packageName;
        }

        if (labels.length == 0) {
            log("No feasible apps found for: " + title, "info");
            Toast.makeText(requireContext(), "No feasible apps found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(labels, (dialog, which) -> onSelected.accept(packageNames[which]))
            .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PICK_APK && resultCode == Activity.RESULT_OK && data != null) {
            Uri apkUri = data.getData();
            if (apkUri != null) silentInstall(apkUri);
        }
    }

    private void silentInstall(Uri uri) {
        try {
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            if (in != null) {
                PackageInstallationUtils.installPackage(requireContext(), in, null);
                log("Silent install session created for APK", "info");
            }
        } catch (Exception e) {
            log("Silent install failed: " + e.getMessage(), "error");
        }
    }

    private void log(String message, String type) {
        LogRepository.appendLog(requireContext(), message, type);
        Intent intent = new Intent(MyDeviceAdminReceiver.ACTION_LOG_EVENT);
        intent.setPackage(requireContext().getPackageName());
        requireContext().sendBroadcast(intent);
    }
}