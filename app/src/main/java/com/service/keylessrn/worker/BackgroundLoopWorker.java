package com.service.keylessrn.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.service.keylessrn.utility.Constants;

import java.util.concurrent.TimeUnit;

public class BackgroundLoopWorker extends Worker {

    WorkHelper workHelper;
    Context context;

    public BackgroundLoopWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        workHelper = new WorkHelper(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        workHelper.launchAppIfNotInForegroundAlready();
        workHelper.doBackgroundWork();
        Log.i(Constants.TAG, "Loop request success!!");
        startBackgroundLoopWorker();

        return Result.success();
    }

    private void startBackgroundLoopWorker() {
        String workTag = this.context.getPackageName() + ".loop";
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(BackgroundLoopWorker.class)
                        .setInitialDelay(30, TimeUnit.SECONDS)
                        .build();

        WorkManager.getInstance(this.context).enqueueUniqueWork(workTag, ExistingWorkPolicy.REPLACE, workRequest);
    }
}
