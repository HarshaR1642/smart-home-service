package com.service.keylessrn.worker;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.service.keylessrn.utility.Constants;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class BackgroundPeriodicWorker extends Worker {

    File downloads;
    public BackgroundPeriodicWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        downloads = getApplicationContext().getFilesDir();
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.i(Constants.TAG, "Background task is running");

        doBackgroundWork();

        return Result.success();
    }

    private void doBackgroundWork() {
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

                            Log.i(Constants.TAG, Constants.CLIENT_APP_PACKAGE + " " + "Local Last Modified: " + localLastModified + " " + "Remote Last Modified: " + remoteLastModified);

                            if (remoteLastModified > localLastModified) {
                                Log.i(Constants.TAG, "Downloading package with size: " + item.getSize());
                                downloadPackage();
                            }else{
                                Log.i(Constants.TAG, "App is upto date with remote");
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
            Amplify.Storage.downloadFile(
                    Constants.SMART_HOME_APK_FILE_NAME,
                    new File(downloads + "/" + Constants.SMART_HOME_APK_FILE_NAME),
                    StorageDownloadFileOptions.defaultInstance(),
                    progress -> Log.i(Constants.TAG, " Current Bytes : " + progress.getCurrentBytes() + " Total Bytes " + progress.getTotalBytes()),
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
            if(!localFile.delete()){
                Log.e(Constants.TAG, "Error in renaming the file");
            };
        }
    }

    private void broadcastDownloadSuccess() {
        Intent downloadSuccessIntent = new Intent(Constants.ACTION_ENABLE_LOCK_MODE_SUCCESS);
        downloadSuccessIntent.setComponent(new ComponentName(Constants.CLIENT_APP_PACKAGE, Constants.CLIENT_APP_RECEIVER_CLASS));
        getApplicationContext().sendBroadcast(downloadSuccessIntent);
    }

    private void installPackage() {
        String apkPath = downloads + "/" + Constants.SMART_HOME_APK_FILE_NAME;

        PackageManager pm = getApplicationContext().getPackageManager();
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
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent.getIntentSender();
    }
}
