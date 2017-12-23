package com.exp.carconnect.basic

import android.app.Application
import com.exp.carconnect.basic.di.CarConnectGlobalComponent
import com.exp.carconnect.basic.di.DaggerCarConnectGlobalComponent
import com.exp.carconnect.basic.di.NewOBDConnectionComponent
import com.exp.carconnect.basic.di.OBDConnectionModule
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject


class CarConnectApp : Application() {
    lateinit var globalComponent: CarConnectGlobalComponent
    @Inject
    lateinit var newConnectionComponentBuilder: NewOBDConnectionComponent.Builder

    var newConnectionComponent: NewOBDConnectionComponent? = null

    override fun onCreate() {
        super.onCreate()
        globalComponent = DaggerCarConnectGlobalComponent.builder().build()
        globalComponent.inject(this)

    }

    fun buildNewConnectionComponent(inputStream: InputStream, outputStream: OutputStream) {
        newConnectionComponent = newConnectionComponentBuilder
                .requestModule(OBDConnectionModule(inputStream, outputStream))
                .build()
    }
}