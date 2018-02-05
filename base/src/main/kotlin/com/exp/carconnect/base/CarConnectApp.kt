package com.exp.carconnect.base

import android.app.Application
import com.exp.carconnect.base.di.CarConnectGlobalComponent
import com.exp.carconnect.base.di.DaggerCarConnectGlobalComponent
import com.exp.carconnect.base.di.NewOBDConnectionComponent
import com.exp.carconnect.base.di.OBDConnectionModule
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject


class CarConnectApp : Application() {
    lateinit var globalComponent: CarConnectGlobalComponent
    @Inject
    lateinit var newConnectionComponentBuilder: NewOBDConnectionComponent.Builder

    companion object {
        const val TAG = "CarConnectApp"
    }

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