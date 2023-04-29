package com.service.keylessrn.api;

import com.service.keylessrn.model.LoginModel;
import com.service.keylessrn.model.LoginResponseModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiInterface {
    @POST("oauth/token")
    Call<LoginResponseModel> login(@Body LoginModel body);
}
