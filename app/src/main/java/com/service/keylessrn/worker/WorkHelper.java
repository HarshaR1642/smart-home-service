package com.service.keylessrn.worker;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.util.Log;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.service.keylessrn.receiver.Receiver;
import com.service.keylessrn.utility.Constants;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WorkHelper {
    Context context;
    File downloads;

    public WorkHelper(Context context) {
        this.context = context;
        downloads = context.getFilesDir();
    }

    protected void registerReceiver() {
        Receiver receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");

        this.context.registerReceiver(receiver, filter);
    }

    protected void doBackgroundWork() {
        StorageListOptions options = StorageListOptions.builder()
                .build();

        Amplify.Storage.list("", options,
                result -> {
                    for (StorageItem item : result.getItems()) {
                        if (item.getKey().equals(Constants.SMART_HOME_APK_FILE_NAME)) {
                            long remoteLastModified = item.getLastModified().getTime() * 1000; // To milliseconds
                            File localFile = new File(downloads, Constants.SMART_HOME_APK_FILE_NAME);
                            long localLastModified = 0;
                            if (localFile.exists()) {
                                localLastModified = localFile.lastModified();
                            }
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                            Date localLastModifiedDate = new Date(localLastModified);
                            Date remoteLastModifiedDate = new Date(remoteLastModified);

                            Log.i(Constants.TAG, Constants.CLIENT_APP_PACKAGE + " " + "Local Last Modified: " + sdf.format(localLastModifiedDate) + " " + "Remote Last Modified: " + sdf.format(remoteLastModifiedDate));

                            if (remoteLastModified > localLastModified) {
                                DecimalFormat df = new DecimalFormat("#.00");
                                Log.i(Constants.TAG, "Downloading package with size: " + df.format(item.getSize() / Math.pow(1000, 2)) + " MB");
                                downloadPackage();
                            } else {
                                Log.i(Constants.TAG, "App is up-to date with remote");
                            }
                        }
                    }
                },
                error -> Log.e(Constants.TAG, "Error in getList", error)
        );
    }

    private void downloadPackage() {
        try {
            boolean isRenamed = renameExistingFile(Constants.SMART_HOME_APK_FILE_NAME, "app.apk");
            if (!isRenamed) {
                return;
            }
            DecimalFormat df = new DecimalFormat("#.00");
            Amplify.Storage.downloadFile(
                    Constants.SMART_HOME_APK_FILE_NAME,
                    new File(downloads + "/" + Constants.SMART_HOME_APK_FILE_NAME),
                    StorageDownloadFileOptions.defaultInstance(),
                    progress -> Log.i(Constants.TAG, "Downloaded: " + df.format(progress.getCurrentBytes() / (Math.pow(1000, 2))) + " MB " + "Total: " + df.format(progress.getTotalBytes() / (Math.pow(1000, 2))) + " MB"),
                    result -> {
                        Log.i(Constants.TAG, "Successfully downloaded: " + result.getFile().getName());
                        deleteFile("app.apk");
                        broadcastDownloadSuccess();
                        installPackage();
                    },
                    error -> {
                        Log.e(Constants.TAG, "Download Failure", error);
                        deleteFile(Constants.SMART_HOME_APK_FILE_NAME);
                        renameExistingFile("app.apk", Constants.SMART_HOME_APK_FILE_NAME);
                    }
            );
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error in deleting local file");
            deleteFile(Constants.SMART_HOME_APK_FILE_NAME);
            renameExistingFile("app.apk", Constants.SMART_HOME_APK_FILE_NAME);
            e.printStackTrace();
        }
    }

    private boolean renameExistingFile(String name, String to) {
        File localFile = new File(downloads + "/" + name);
        if (localFile.exists()) {
            File renameFile = new File(downloads + "/" + to);
            if (!localFile.renameTo(renameFile)) {
                Log.e(Constants.TAG, "Error in renaming the file");
                return false;
            }
        }
        return true;
    }

    private void deleteFile(String fileName) {
        File localFile = new File(downloads + "/" + fileName);
        if (localFile.exists()) {
            if (!localFile.delete()) {
                Log.e(Constants.TAG, "Error in renaming the file");
            }
        }
    }

    private void broadcastDownloadSuccess() {
        Intent downloadSuccessIntent = new Intent(Constants.ACTION_ENABLE_LOCK_MODE_SUCCESS);
        downloadSuccessIntent.setComponent(new ComponentName(Constants.CLIENT_APP_PACKAGE, Constants.CLIENT_APP_RECEIVER_CLASS));
        this.context.sendBroadcast(downloadSuccessIntent);
    }

    protected void installPackage() {
        String apkPath = downloads + "/" + Constants.SMART_HOME_APK_FILE_NAME;
        File localFile = new File(apkPath);
        if (!localFile.exists()) {
            return;
        }

        PackageManager pm = this.context.getPackageManager();
        PackageInstaller packageInstaller = pm.getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        try {
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            File apkFile = new File(apkPath);
            OutputStream out = session.openWrite(Constants.CLIENT_APP_PACKAGE, 0, -1);
            InputStream in = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                in = Files.newInputStream(apkFile.toPath());
            }
            byte[] buffer = new byte[65536];
            int c;
            if (in != null) {
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
            }
            session.fsync(out);
            if (in != null) {
                in.close();
            }
            out.close();
            session.commit(createIntentSender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IntentSender createIntentSender() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this.context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent.getIntentSender();
    }

    private boolean isAppInForeground() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 1000 * 10, time);
        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            UsageStats usageStats = usageStatsList.get(0);
            return usageStats.getPackageName().equals(Constants.CLIENT_APP_PACKAGE);
        }
        return false;
    }

    protected void launchAppIfNotInForegroundAlready() {
        if (!isAppInForeground()) {
            Intent launchIntent = this.context.getPackageManager().getLaunchIntentForPackage(Constants.CLIENT_APP_PACKAGE);
            if (launchIntent == null) {
                installPackage();
            } else {
                Intent broadcastIntent = new Intent(Constants.ACTION_LAUNCH_CLIENT_APP);
                broadcastIntent.setClassName("com.service.keylessrn", "com.service.keylessrn.receiver.Receiver");
                this.context.sendBroadcast(broadcastIntent);
            }
        } else {
            Log.i(Constants.TAG, "App is in foreground");
        }
    }
}
