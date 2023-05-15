package com.service.keylessrn.receiver;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.service.keylessrn.worker.BackgroundOneTimeWorker;
import com.service.keylessrn.worker.BackgroundPeriodicWorker;
import com.service.keylessrn.utility.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(Constants.TAG, "Broadcast Received " + action);
        switch (action) {
            case Constants.ACTION_ENABLE_LOCK_MODE:
                try {
                    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                    if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                        ComponentName admin = new ComponentName(context, DeviceAdminReceiver.class);
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
                break;
            case Constants.ACTION_DISABLE_LOCK_MODE:
                try {
                    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                        dpm.clearDeviceOwnerApp(context.getPackageName());
                        ComponentName admin = new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
                        dpm.removeActiveAdmin(admin);
                        Log.i(Constants.TAG, "Removed as device owner");
                    } else {
                        Log.i(Constants.TAG, "App is not an owner");
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Error in disableLockMode");
                    e.printStackTrace();
                }
                break;
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REPLACED:
                Log.i(Constants.TAG, "Package Installed");
                String packageName = intent.getData().getSchemeSpecificPart();
                Log.i(Constants.TAG, packageName);
                if (packageName.equals(Constants.CLIENT_APP_PACKAGE)) {
                    try {
                        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                        if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                            ComponentName admin = new ComponentName(context, DeviceAdminReceiver.class);
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
                break;
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(Constants.CLIENT_APP_PACKAGE);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                }
                startBackgroundOneTimeWorker(context);
                startBackgroundPeriodicWorker(context);
                break;
        }
    }

    private void startBackgroundOneTimeWorker(Context context) {
        String workTag = context.getPackageName() + ".onetime";
        boolean isRunning = checkWorkStatus(context, workTag);
        if (isRunning) {
            return;
        }
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(BackgroundOneTimeWorker.class)
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(workTag, ExistingWorkPolicy.KEEP, workRequest);
    }

    private void startBackgroundPeriodicWorker(Context context) {
        String workTag = context.getPackageName() + ".periodic";
        boolean isRunning = checkWorkStatus(context, workTag);
        if (isRunning) {
            return;
        }
        PeriodicWorkRequest.Builder workRequestBuilder =
                new PeriodicWorkRequest.Builder(BackgroundPeriodicWorker.class, 15, TimeUnit.MINUTES);
        PeriodicWorkRequest workRequest = workRequestBuilder.addTag(workTag).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(workTag, ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    private boolean checkWorkStatus(Context context, String tag) {
        WorkManager workManager = WorkManager.getInstance(context);

        ListenableFuture<List<WorkInfo>> statuses = workManager.getWorkInfosByTag(tag);
        try {
            List<WorkInfo> workInfoList = statuses.get();
            if (!workInfoList.isEmpty()) {
                WorkInfo workInfo = workInfoList.get(0);
                return workInfo.getState() == WorkInfo.State.RUNNING;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}