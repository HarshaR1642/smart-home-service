package com.service.keylessrn.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {

    private final static String BASE_URL = "https://app2.keyless.rocks/";
    private final static String AUTH_URL = "https://remotapp.rently.com/";

    private static Retrofit retrofitInstance = null;

    public static ApiInterface getRetrofitInstance(){
        if(retrofitInstance == null){
            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofitInstance.create(ApiInterface.class);
    }

    public static ApiInterface getRetrofitLoginInstance(){
        if(retrofitInstance == null){
            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(AUTH_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofitInstance.create(ApiInterface.class);
    }
}