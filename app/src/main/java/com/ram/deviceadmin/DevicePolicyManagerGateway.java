package com.ram.deviceadmin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class DevicePolicyManagerGateway {
    private final Context context;
    private final DevicePolicyManager dpm;
    private final UserManager userManager;
    private final ComponentName adminComponent;

    public DevicePolicyManagerGateway(Context context) {
        this.context = context.getApplicationContext();
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        this.adminComponent = new ComponentName(context, MyDeviceAdminReceiver.class);
    }

    public ComponentName getAdmin() {
        return adminComponent;
    }

    public DevicePolicyManager getDpm() {
        return dpm;
    }

    public boolean isDeviceOwner() {
        return dpm.isDeviceOwnerApp(context.getPackageName());
    }

    public void setUserRestriction(String restriction, boolean enabled, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            if (enabled) {
                dpm.addUserRestriction(adminComponent, restriction);
            } else {
                dpm.clearUserRestriction(adminComponent, restriction);
            }
            onSuccess.accept("Restriction " + restriction + " set to " + enabled);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setUninstallBlocked(String packageName, boolean blocked, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setUninstallBlocked(adminComponent, packageName, blocked);
            onSuccess.accept("Uninstall blocked for " + packageName + ": " + blocked);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public boolean isUninstallBlocked(String packageName) {
        return dpm.isUninstallBlocked(adminComponent, packageName);
    }

    public void setApplicationHidden(String packageName, boolean hidden, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            boolean success = dpm.setApplicationHidden(adminComponent, packageName, hidden);
            if (success) onSuccess.accept("App " + packageName + " hidden: " + hidden);
            else onError.accept(new Exception("Failed to hide/unhide app"));
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setPackagesSuspended(String[] packageNames, boolean suspended, Consumer<String[]> onSuccess, Consumer<Exception> onError) {
        try {
            String[] failed = dpm.setPackagesSuspended(adminComponent, packageNames, suspended);
            onSuccess.accept(failed);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setCameraDisabled(boolean disabled, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setCameraDisabled(adminComponent, disabled);
            onSuccess.accept("Camera disabled: " + disabled);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setScreenCaptureDisabled(boolean disabled, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setScreenCaptureDisabled(adminComponent, disabled);
            onSuccess.accept("Screen capture disabled: " + disabled);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setMasterVolumeMuted(boolean muted, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setMasterVolumeMuted(adminComponent, muted);
            onSuccess.accept("Master volume muted: " + muted);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setPermissionPolicy(int policy, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setPermissionPolicy(adminComponent, policy);
            onSuccess.accept("Permission policy set to: " + policy);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setPermissionGrantState(String packageName, String permission, int state, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            boolean success = dpm.setPermissionGrantState(adminComponent, packageName, permission, state);
            if (success) onSuccess.accept("Permission " + permission + " for " + packageName + " set to state: " + state);
            else onError.accept(new Exception("Failed to set permission grant state"));
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setKeyguardDisabled(boolean disabled, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            boolean success = dpm.setKeyguardDisabled(adminComponent, disabled);
            onSuccess.accept("Keyguard disabled: " + disabled + " (Result: " + success + ")");
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setPermittedAccessibilityServices(List<String> packageNames, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            boolean success = dpm.setPermittedAccessibilityServices(adminComponent, packageNames);
            onSuccess.accept("Permitted accessibility services updated (Result: " + success + ")");
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setPermittedInputMethods(List<String> packageNames, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            boolean success = dpm.setPermittedInputMethods(adminComponent, packageNames);
            onSuccess.accept("Permitted input methods updated (Result: " + success + ")");
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setDelegatedScopes(String delegatePackage, List<String> scopes, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setDelegatedScopes(adminComponent, delegatePackage, scopes);
            onSuccess.accept("Delegated scopes for " + delegatePackage + " set to: " + scopes);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setGlobalSetting(String setting, String value, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setGlobalSetting(adminComponent, setting, value);
            onSuccess.accept("Global setting " + setting + " set to " + value);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setSecureSetting(String setting, String value, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setSecureSetting(adminComponent, setting, value);
            onSuccess.accept("Secure setting " + setting + " set to " + value);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void setSystemSetting(String setting, String value, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.setSystemSetting(adminComponent, setting, value);
            onSuccess.accept("System setting " + setting + " set to " + value);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void lockNow(Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.lockNow();
            onSuccess.accept("Device locked");
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void wipeData(int flags, Consumer<String> onSuccess, Consumer<Exception> onError) {
        try {
            dpm.wipeData(flags);
            onSuccess.accept("Wipe data triggered");
        } catch (Exception e) {
            onError.accept(e);
        }
    }
}