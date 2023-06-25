package com.service.keylessrn.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.service.keylessrn.utility.Constants;

public class BackgroundPeriodicWorker extends Worker {
    WorkHelper workHelper;

    public BackgroundPeriodicWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        workHelper = new WorkHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {

        workHelper.launchAppIfNotInForegroundAlready();
        workHelper.doBackgroundWork();

        Log.i(Constants.TAG, "Periodic request success!!");

        return Result.success();
    }
}
