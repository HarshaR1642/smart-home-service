package com.service.keylessrn;
import com.service.keylessrn.model.LoginResponseModel;

interface ResponseCallback {
    void onResponse(in LoginResponseModel response);
}