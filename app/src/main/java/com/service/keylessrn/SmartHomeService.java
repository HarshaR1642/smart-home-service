package com.service.keylessrn;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.service.keylessrn.api.ApiInterface;
import com.service.keylessrn.api.RetrofitInstance;
import com.service.keylessrn.model.LoginModel;
import com.service.keylessrn.model.LoginResponseModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmartHomeService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public V5AidlInterface.Stub binder = new V5AidlInterface.Stub() {

        @Override
        public void login(Bundle bundle, ResponseCallback callback) {
            String email = bundle.getString("email");
            String password = bundle.getString("password");

            LoginModel loginModel = new LoginModel(email, password);
            ApiInterface retrofitInstance = RetrofitInstance.getRetrofitLoginInstance();
            Call<LoginResponseModel> call = retrofitInstance.login(loginModel);

            call.enqueue(new Callback<LoginResponseModel>() {
                @Override
                public void onResponse(@NonNull Call<LoginResponseModel> call, @NonNull Response<LoginResponseModel> response) {

                    if(response.body() != null){
                        try {
                            callback.onResponse(response.body());
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoginResponseModel> call, @NonNull Throwable t) {
                }
            });
        }
    };

}