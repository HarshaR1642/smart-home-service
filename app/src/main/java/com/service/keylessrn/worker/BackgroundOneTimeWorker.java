package com.service.keylessrn.worker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.service.keylessrn.receiver.Receiver;
import com.service.keylessrn.utility.Constants;

public class BackgroundOneTimeWorker extends Worker {

    public BackgroundOneTimeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Receiver receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");

        getApplicationContext().registerReceiver(receiver, filter);

        Log.i(Constants.TAG, "One time request success!!");

        return Result.success();
    }
}
