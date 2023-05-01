package com.service.keylessrn.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

public class ServiceJobScheduler extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
