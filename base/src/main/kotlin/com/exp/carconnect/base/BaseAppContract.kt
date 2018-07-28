package com.exp.carconnect.base

import android.content.SharedPreferences
import com.exp.carconnect.base.di.NewOBDConnectionComponent
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.base.store.BaseStore
import com.google.gson.Gson
import io.reactivex.Scheduler
import redux.api.Store
import java.io.InputStream
import java.io.OutputStream


interface BaseAppContract {

    fun buildNewOBDConnectionComponent(inputStream: InputStream, outputStream: OutputStream): NewOBDConnectionComponent
    var newOBDConnectionComponent: NewOBDConnectionComponent?
    fun onDataLoadingStartedFor(info: Vehicle)
    fun onReportRequested()

    fun killSession()

    val store: Store<AppState>
    val persistenceStore: BaseStore

    val mainScheduler: Scheduler
    val ioScheduler: Scheduler
    val computationScheduler: Scheduler

}