package com.service.keylessrn;
import com.service.keylessrn.ResponseCallback;

interface V5AidlInterface {

    void login(inout Bundle bundle, ResponseCallback responseCallback);
}