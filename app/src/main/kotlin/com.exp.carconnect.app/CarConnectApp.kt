package com.exp.carconnect.app

import com.exp.carconnect.base.CarConnectAbstractApp


class CarConnectApp : CarConnectAbstractApp() {

    override fun getMode(): String {
       return "CompleteMainApplication"
    }
}