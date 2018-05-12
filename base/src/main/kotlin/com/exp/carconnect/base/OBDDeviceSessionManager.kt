package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.Logger
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.OBDMultiRequest
import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Observable
import io.reactivex.Scheduler
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
        const val TAG = "OBDSession"
        private const val INITIALIZATION = "Initialization"
        private const val VEHICLE_INFO = "Vehicle Info"
        private val setupRequest = OBDMultiRequest(INITIALIZATION,
                listOf(EchoOffCommand(), EchoOffCommand(), LineFeedOffCommand(), TimeoutCommand(62)))
        private val resetCommand = OBDResetCommand()

        private val vehicleInfoRequest = OBDMultiRequest(VEHICLE_INFO, listOf(VinRequest(),
                FuelTypeRequest(),
                AvailablePidsCommand(PidCommand.ONE_TO_TWENTY),
                AvailablePidsCommand(PidCommand.TWENTY_ONE_TO_FOURTY),
                AvailablePidsCommand(PidCommand.FOURTY_ONE_TO_SIXTY)))
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
                            startOBDSession(it.socket, it.device)
                                    .startWith(it)

                        }
                        else -> {
                            Observable.just(it)
                        }
                    }
                }
    }


    private fun startOBDSession(socket: BluetoothSocket, device: BluetoothDevice): Observable<CommonAppAction> {
        val engine = OBDEngine(socket.inputStream, socket.outputStream,
                computationScheduler, ioScheduler)
        return Observable.concat(executeSetupCommands(engine), loadVehicleInfo(engine))
    }


    private fun executeSetupCommands(engine: OBDEngine): Observable<CommonAppAction> {
        return engine.submit<OBDResponse>(resetCommand)
                .doOnNext {
                    Logger.log(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
                }
                .flatMap {
                    engine
                            .submit<OBDResponse>(setupRequest)
                            .delaySubscription(500, TimeUnit.MILLISECONDS)
                }
                .lastOrError()
                .toObservable()
                .map {
                    CommonAppAction.SetupCompleted(device) as CommonAppAction
                }
                .onErrorReturn { CommonAppAction.SetupFailed(device, it) }
                .startWith(CommonAppAction.RunningSetup(device))

    }


    private fun loadVehicleInfo(engine: OBDEngine): Observable<CommonAppAction> {
        return engine.submit<OBDResponse>(vehicleInfoRequest)
                .doOnNext { Logger.log(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]") }
                .toList()
                .map { responses ->
                    var vin: String? = null
                    var fuelType: FuelType? = null
                    var availablePIDs = mutableSetOf<String>()
                    responses.forEach {
                        when (it) {
                            is VinResponse -> {
                                vin = it.vin
                            }
                            is FuelTypeResponse -> {
                                fuelType = FuelType.fromValue(it.fuelType)
                            }
                            is RawResponse -> {
                                Logger.log(TAG, "Available pids response " + it.rawResponse)
                            }
                        }
                    }

                    CommonAppAction.VehicleInfoLoaded(device,
                            Vehicle(vin!!, UnAvailableAvailableData.UnAvailable, UnAvailableAvailableData.Available(fuelType!!))) as CommonAppAction
                }
                .toObservable()
                .onErrorReturn { CommonAppAction.VehicleInfoLoadingFailed(device, it) }
                .startWith(CommonAppAction.LoadingVehicleInfo(device))
    }


}