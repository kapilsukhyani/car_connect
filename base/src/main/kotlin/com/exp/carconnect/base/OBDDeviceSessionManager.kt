package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.Logger
import com.exp.carconnect.base.state.BaseAppAction
import com.exp.carconnect.base.state.FreezeFrame
import com.exp.carconnect.base.state.MILStatus
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.OBDMultiRequest
import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

class OBDDeviceSessionManager(private val ioScheduler: Scheduler,
                              private val computationScheduler: Scheduler,
                              private val mainScheduler: Scheduler) {


    fun startNewSession(device: BluetoothDevice): Observable<BaseAppAction> {
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


        private const val FUEL_FACTOR = 100.0f
        private const val RPM_FACTOR = 1000.0f
        private val fuelLevelRequest = FuelLevelRequest(repeatable = IsRepeatable.Yes(1, TimeUnit.MINUTES))
        private val temperatureRequest = OBDMultiRequest("Temperature", listOf(TemperatureRequest(TemperatureType.AIR_INTAKE),
                TemperatureRequest(TemperatureType.AMBIENT_AIR)),
                IsRepeatable.Yes(500, TimeUnit.MILLISECONDS))
        private val dtcNumberRequest = DTCNumberRequest(repeatable = IsRepeatable.Yes(1, TimeUnit.MINUTES))
        private val pendingTroubleCodesRequest = PendingTroubleCodesRequest(TroubleCodeCommandType.ALL)
        private val fastChangingDataRequest = OBDMultiRequest("Dashboard",
                listOf(SpeedRequest(),
                        RPMRequest(),
                        IgnitionMonitorRequest()),
                IsRepeatable.Yes(50, TimeUnit.MILLISECONDS))
        private val freezeFrameRequest = OBDMultiRequest("Dashboard",
                listOf(SpeedRequest(mode = OBDRequestMode.FREEZE_FRAME),
                        RPMRequest(mode = OBDRequestMode.FREEZE_FRAME)))

    }

    fun start(): Observable<BaseAppAction> {
        return Observable
                .fromCallable {
                    try {
                        val socket = device.connect()
                        val engine = OBDEngine(socket.inputStream, socket.outputStream,
                                computationScheduler, ioScheduler)
                        BaseAppAction.AddActiveSession(device, socket, engine)
                    } catch (e: Throwable) {
                        BaseAppAction.DeviceConnectionFailed(device, e)
                    }

                }.flatMap {
                    when (it) {
                        is BaseAppAction.AddActiveSession -> {
                            startOBDSession(it.socket, it.device, it.engine)
                                    .startWith(it)

                        }
                        else -> {
                            Observable.just(it)
                        }
                    }
                }
    }


    private fun startOBDSession(socket: BluetoothSocket,
                                device: BluetoothDevice,
                                engine: OBDEngine): Observable<BaseAppAction> {
        return Observable.concat(executeSetupCommands(engine),
                loadVehicleInfo(engine),
                loadVehicleData(engine))
    }


    private fun executeSetupCommands(engine: OBDEngine): Observable<BaseAppAction> {
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
                    BaseAppAction.SetupCompleted(device) as BaseAppAction
                }
                .onErrorReturn { BaseAppAction.SetupFailed(device, it) }
                .startWith(BaseAppAction.RunningSetup(device))

    }


    private fun loadVehicleInfo(engine: OBDEngine): Observable<BaseAppAction> {
        return engine.submit<OBDResponse>(vehicleInfoRequest)
                .doOnNext { Logger.log(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]") }
                .toList()
                .map { responses ->
                    var vin: String? = null
                    var fuelType: FuelType? = null
                    val availablePIDs = mutableSetOf<String>()
                    responses.forEach {
                        when (it) {
                            is VinResponse -> {
                                vin = it.vin
                            }
                            is FuelTypeResponse -> {
                                fuelType = FuelType.fromValue(it.fuelType)
                            }
                            is AvailablePidsResponse -> {
                                Logger.log(TAG, "Available pids response " + it.availablePids)
                                availablePIDs.addAll(it.availablePids)
                            }
                        }
                    }

                    BaseAppAction.AddVehicleInfoToActiveSession(device,
                            Vehicle(vin!!, UnAvailableAvailableData.Available(availablePIDs),
                                    UnAvailableAvailableData.Available(fuelType!!))) as BaseAppAction
                }
                .toObservable()
                .onErrorReturn { BaseAppAction.VehicleInfoLoadingFailed(device, it) }
                .startWith(BaseAppAction.LoadingVehicleInfo(device))
    }

    private fun loadVehicleData(engine: OBDEngine): Observable<BaseAppAction> {
        return engine.submit<TemperatureResponse>(temperatureRequest)
                .map<BaseAppAction> {
                    if (it.type == TemperatureType.AIR_INTAKE) {
                        BaseAppAction.AddAirIntakeTemperature(it.temperature.toFloat())
                    } else {
                        BaseAppAction.AddAmbientAirTemperature(it.temperature.toFloat())
                    }
                }
                .mergeWith(engine.submit<FuelLevelResponse>(fuelLevelRequest)
                        .map {
                            BaseAppAction.AddFuel(it.fuelLevel / FUEL_FACTOR)
                        })

                .mergeWith(engine.submit<OBDResponse>(fastChangingDataRequest)
                        .map { response ->
                            when (response) {
                                is SpeedResponse -> BaseAppAction.AddSpeed(response.metricSpeed.toFloat())
                                is RPMResponse -> BaseAppAction.AddRPM(response.rpm / RPM_FACTOR)
                                else -> BaseAppAction.AddIgnition((response as IgnitionMonitorResponse).ignitionOn)
                            }
                        })
                .mergeWith(
                        engine.submit<DTCNumberResponse>(dtcNumberRequest)
                                .flatMap { dtcResponse ->
                                    if (dtcResponse.milOn) {
                                        engine
                                                .submit<PendingTroubleCodesResponse>(pendingTroubleCodesRequest)
                                                .zipWith(engine.submit<OBDResponse>(freezeFrameRequest)
                                                        .toList()
                                                        .map {
                                                            var rpm = 0
                                                            var speed = 0
                                                            for (response in it) {
                                                                if (response is SpeedResponse) {
                                                                    speed = response.metricSpeed
                                                                } else if (response is RPMResponse) {
                                                                    rpm = response.rpm
                                                                }
                                                            }

                                                            FreezeFrame(rpm, speed)
                                                        }.toObservable()
                                                        , BiFunction<PendingTroubleCodesResponse, FreezeFrame, Pair<PendingTroubleCodesResponse, FreezeFrame>> { t1, t2 ->
                                                    Pair(t1, t2)
                                                })

                                                .map {
                                                    MILStatus.On(it.first.codes, UnAvailableAvailableData.Available(it.second))
                                                }
                                    } else {
                                        Observable.just(MILStatus.Off)
                                    }
                                }.map {
                                    BaseAppAction.AddMilStatus(it)
                                }
                )
                .onErrorReturn {
                    BaseAppAction.AddVehicleDataLoadError(it)
                }


    }
}

