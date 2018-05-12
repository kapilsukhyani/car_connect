package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.Logger
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.OBDMultiRequest
import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.util.concurrent.TimeUnit

class OBDDeviceSessionManager(private val ioScheduler: Scheduler,
                              private val computationScheduler: Scheduler,
                              private val mainScheduler: Scheduler) {


    fun startNewSession(device: BluetoothDevice): Observable<CommonAppAction> {
        return OBDSession(device,
                ioScheduler,
                computationScheduler,
                mainScheduler).start()
    }

}

class OBDSession(val device: BluetoothDevice,
                 private val ioScheduler: Scheduler,
                 private val computationScheduler: Scheduler,
                 private val mainScheduler: Scheduler) {

    companion object {
        const val TAG = "SetupScreenVM"
        private const val INITIALIZATION = "Initialization"
        private val setupRequest = OBDMultiRequest(INITIALIZATION,
                listOf(EchoOffCommand(), EchoOffCommand(), LineFeedOffCommand(), TimeoutCommand(62)))
        private val resetCommand = OBDResetCommand()
    }

    fun start(): Observable<CommonAppAction> {
        return Observable
                .fromCallable {
                    try {
                        val socket = device.connect()
                        CommonAppAction.DeviceConnected(device, socket)
                    } catch (e: Throwable) {
                        CommonAppAction.DeviceConnectionFailed(device, e)
                    }

                }.flatMap {
                    when (it) {
                        is CommonAppAction.DeviceConnected -> {
                            startSetup(it.socket, it.device)
                                    .startWith(it)

                        }
                        else -> {
                            Observable.just(it)
                        }
                    }
                }
    }


    private fun startSetup(socket: BluetoothSocket, device: BluetoothDevice): Observable<CommonAppAction> {
        return executeSetupCommands(OBDEngine(socket.inputStream, socket.outputStream,
                computationScheduler, ioScheduler))
                .toObservable()
                .map {
                    CommonAppAction.SetupCompleted(device) as CommonAppAction
                }
                .onErrorReturn { CommonAppAction.SetupFailed(device, it) }
                .startWith(CommonAppAction.RunningSetup(device))

    }


    private fun executeSetupCommands(engine: OBDEngine): Single<OBDResponse> {
        return engine.submit<OBDResponse>(resetCommand)
                .observeOn(mainScheduler)
                .doOnNext {
                    Logger.log(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
                }
                .flatMap {
                    engine
                            .submit<OBDResponse>(setupRequest)
                            .delaySubscription(500, TimeUnit.MILLISECONDS)
                            .observeOn(mainScheduler)
                }
                .lastOrError()

    }


}