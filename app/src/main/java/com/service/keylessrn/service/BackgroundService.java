package com.service.keylessrn.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.service.keylessrn.utility.Constants;
import com.service.keylessrn.receiver.Receiver;

public class BackgroundService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Constants.TAG, "Started Background Service");
        Receiver receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");

        registerReceiver(receiver, filter);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}