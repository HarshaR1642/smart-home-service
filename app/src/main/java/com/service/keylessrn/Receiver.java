package com.service.keylessrn;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(Constants.TAG, "Broadcast Received " + action);
        if (action.equals(Constants.ACTION_ENABLE_LOCK_MODE)) {
            try {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                    ComponentName admin = new ComponentName(context, SmartHomeDeviceAdminReceiver.class);
                    dpm.setLockTaskPackages(admin, new String[]{Constants.CLIENT_APP_PACKAGE});
                    Intent successIntent = new Intent(Constants.ACTION_ENABLE_LOCK_MODE_SUCCESS);
                    successIntent.setComponent(new ComponentName(Constants.CLIENT_APP_PACKAGE, Constants.CLIENT_APP_RECEIVER_CLASS));
                    context.sendBroadcast(successIntent);
                } else {
                    Log.i(Constants.TAG, "App is not an owner");
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "Error in enableLockMode");
                e.printStackTrace();
            }
        } else if (action.equals(Constants.ACTION_DISABLE_LOCK_MODE)) {
            try {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                    dpm.clearDeviceOwnerApp(context.getPackageName());
                    ComponentName admin = new ComponentName(context.getApplicationContext(), SmartHomeDeviceAdminReceiver.class);
                    dpm.removeActiveAdmin(admin);
                    Log.i(Constants.TAG, "Removed as device owner");
                } else {
                    Log.i(Constants.TAG, "App is not an owner");
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "Error in disableLockMode");
                e.printStackTrace();
            }
        } else if (action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            Log.i(Constants.TAG, packageName);
            if (packageName.equals(Constants.CLIENT_APP_PACKAGE)) {
                try {
                    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                    if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                        ComponentName admin = new ComponentName(context, SmartHomeDeviceAdminReceiver.class);
                        dpm.setLockTaskPackages(admin, new String[]{Constants.CLIENT_APP_PACKAGE});
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(Constants.CLIENT_APP_PACKAGE);
                        if (launchIntent != null) {
                            context.startActivity(launchIntent);
                        }
                    } else {
                        Log.i(Constants.TAG, "App is not an owner");
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Error in enableLockMode");
                    e.printStackTrace();
                }
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(Constants.CLIENT_APP_PACKAGE);
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            }
        }
    }
}