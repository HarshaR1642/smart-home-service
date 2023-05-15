package com.service.keylessrn;

import android.app.Application;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.service.keylessrn.utility.Constants;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeAmplify();
    }

    private void initializeAmplify() {
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSS3StoragePlugin());
            try {
                Amplify.configure(getApplicationContext());
            } catch (AmplifyException e) {
                e.printStackTrace();
            }
            Log.i(Constants.TAG, "Initialized Amplify");
        } catch (AmplifyException error) {
            Log.e(Constants.TAG, "Could not initialize Amplify", error);
        }
    }
}
