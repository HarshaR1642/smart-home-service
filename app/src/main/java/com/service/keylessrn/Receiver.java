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
        switch (action) {
            case Constants.ACTION_ENABLE_LOCK_MODE: {
                try {
                    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                    if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                        ComponentName admin = new ComponentName(context, SmartHomeDeviceAdminReceiver.class);
                        dpm.setLockTaskPackages(admin, new String[]{"rocks.keyless.app.android"});
                        Intent successIntent = new Intent(Constants.ACTION_ENABLE_LOCK_MODE_SUCCESS);
                        successIntent.setComponent(new ComponentName("rocks.keyless.app.android", "com.rently.keylessrn.Receiver"));
                        context.sendBroadcast(successIntent);
                    } else {
                        Log.i(Constants.TAG, "App is not an owner");
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Error in enableLockMode");
                    e.printStackTrace();
                }
                break;
            }

            case Constants.ACTION_DISABLE_LOCK_MODE: {
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
            }
        }
    }
}