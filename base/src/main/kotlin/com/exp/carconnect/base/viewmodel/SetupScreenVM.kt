package com.exp.carconnect.base.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.exp.carconnect.Logger
import com.exp.carconnect.base.CarConnectAbstractApp
import com.exp.carconnect.base.connect
import com.exp.carconnect.base.di.Main
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.OBDMultiRequest
import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SetupScreenVM(app: Application) : AndroidViewModel(app) {
    companion object {
        val TAG = "SetupScreenVM"
        private val INITIALIZATION = "Initialization"
        private val setupRequest = OBDMultiRequest(INITIALIZATION,
                listOf(EchoOffCommand(), EchoOffCommand(), LineFeedOffCommand(), TimeoutCommand(62)))
        private val resetCommand = OBDResetCommand()
    }


    private val subscriptions = CompositeDisposable()
    private val mutableSetupStatusLiveData = MutableLiveData<SetupStatus>()
    private var socket: BluetoothSocket? = null
    val setupStatusLiveData = mutableSetupStatusLiveData

    @Inject
    lateinit var obdEngine: OBDEngine
    @Inject
    @field:Main
    lateinit var mainScheduler: Scheduler


    fun initConnection() {
        mutableSetupStatusLiveData.value = SetupStatus.InProgress("Connecting to device")
        connect()?.let {
            socket = it
            setupNewOBDConnectionComponent(it)
            startOBDInitialization()
        } ?: connectionFailed()


    }

    private fun connectionFailed() {
        mutableSetupStatusLiveData.value = SetupStatus.InProgress("Could not connect")
    }

    private fun startOBDInitialization() {
        mutableSetupStatusLiveData.value = SetupStatus.InProgress("Sending init requests")
        subscriptions.add(
                obdEngine.submit<OBDResponse>(SetupScreenVM.resetCommand)
                        .observeOn(mainScheduler)
                        .doOnNext {
                            Log.d(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
                            mutableSetupStatusLiveData.value = SetupStatus.InProgress(it.getFormattedResult())
                        }
                        .flatMap {
                            obdEngine
                                    .submit<OBDResponse>(SetupScreenVM.setupRequest)
                                    .delaySubscription(500, TimeUnit.MILLISECONDS)
                                    .observeOn(mainScheduler)
                        }
                        .subscribe(
                                {
                                    Logger.log(SetupScreenVM.TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
                                    mutableSetupStatusLiveData.value = SetupStatus.InProgress(it.getFormattedResult())
                                },
                                {
                                    Logger.log(SetupScreenVM.TAG, "Got Error ${it::class.java.simpleName}[${it.message}]")
                                    mutableSetupStatusLiveData.value = SetupStatus.Error("Unable to setup", it)
                                },
                                {
                                    mutableSetupStatusLiveData.value = SetupStatus.Completed
                                }))
    }


    private fun connect(): BluetoothSocket? {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.bondedDevices?.let {
            if (adapter.isEnabled) {
                return it.filter { it.name?.contains("OBD")!! }[0].connect()
            }

        }
        return null
    }

    private fun setupNewOBDConnectionComponent(socket: BluetoothSocket) {
        getApplication<CarConnectAbstractApp>().buildNewConnectionComponent(socket.inputStream, socket.outputStream)
        getApplication<CarConnectAbstractApp>().newConnectionComponent!!.inject(this)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.dispose()
        //todo this cannot be done anymore since setup is just to initiate the connection and after that it other screens take over
//        socket?.close()
    }
}


sealed class SetupStatus {
    object Completed : SetupStatus()
    data class Error(val errorMessage: String, val error: Throwable) : SetupStatus()
    data class InProgress(val state: String) : SetupStatus()
}