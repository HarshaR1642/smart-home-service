package com.service.keylessrn.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.service.keylessrn.utility.Constants;

public class BackgroundOneTimeWorker extends Worker {

    WorkHelper workHelper;

    public BackgroundOneTimeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        workHelper = new WorkHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {

        workHelper.registerReceiver();

        Log.i(Constants.TAG, "One time request success!!");

        return Result.success();
    }
}
