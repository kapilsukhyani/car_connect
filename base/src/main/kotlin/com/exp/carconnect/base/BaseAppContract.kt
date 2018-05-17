package com.exp.carconnect.base

import com.exp.carconnect.base.di.NewOBDConnectionComponent
import redux.api.Store
import java.io.InputStream
import java.io.OutputStream


interface BaseAppContract {

    fun buildNewOBDConnectionComponent(inputStream: InputStream, outputStream: OutputStream): NewOBDConnectionComponent
    var newOBDConnectionComponent: NewOBDConnectionComponent?
    val store: Store<AppState>

}