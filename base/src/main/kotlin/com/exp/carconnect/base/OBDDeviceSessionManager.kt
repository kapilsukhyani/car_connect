package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.Logger
import com.exp.carconnect.base.state.*
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
    companion object {
        const val TAG = "OBDDeviceSessionManager"
    }

    private var activeSession: OBDSession? = null

    fun startNewSession(device: BluetoothDevice,
                        settings: AppSettings): Observable<BaseAppAction> {
        activeSession = OBDSession(device,
                ioScheduler,
                computationScheduler,
                mainScheduler)

        return activeSession!!
                .start(settings)
                .doOnDispose { Logger.log(TAG, "Active session data loading stopped") }
    }

    fun killActiveSession() {
        Logger.log(TAG, "Killing an active session")
        activeSession = null
    }

    fun updateDataLoadFrequencyForActiveSession(engine: OBDEngine,
                                                settings: AppSettings): Observable<BaseAppAction> {
        return activeSession
                ?.loadVehicleData(engine, settings)
                ?.doOnDispose { Logger.log(TAG, "Active session data loading stopped") }
                ?: Observable.empty()
    }

    fun clearDTCs(engine: OBDEngine): Observable<BaseAppAction> {
        return activeSession
                ?.clearDTCs(engine)
                ?: Observable.empty()
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
        private const val FAST_CHANGING_DATA = "Fast Changing Data"
        private const val TEMPERATURE = "Temperature"
        private const val FREEZE_FRAME = "Freeze Frame"
        private const val FUEL_FACTOR = 100.0f
        private const val RPM_FACTOR = 1000.0f

        private val setupRequest = OBDMultiRequest(INITIALIZATION,
                listOf(EchoOffCommand(), EchoOffCommand(), LineFeedOffCommand(), TimeoutCommand(62)))
        private val resetCommand = OBDResetCommand()

        private val vehicleInfoRequest = OBDMultiRequest(VEHICLE_INFO, listOf(VinRequest(),
                FuelTypeRequest(),
                AvailablePidsCommand(PidCommand.ONE_TO_TWENTY),
                AvailablePidsCommand(PidCommand.TWENTY_ONE_TO_FOURTY),
                AvailablePidsCommand(PidCommand.FOURTY_ONE_TO_SIXTY)))

        private val dtcNumberRequest = DTCNumberRequest(repeatable = IsRepeatable.Yes(1, TimeUnit.MINUTES))
        private val pendingTroubleCodesRequest = PendingTroubleCodesRequest(TroubleCodeCommandType.ALL)
        private val freezeFrameRequest = OBDMultiRequest(FREEZE_FRAME,
                listOf(SpeedRequest(mode = OBDRequestMode.FREEZE_FRAME),
                        RPMRequest(mode = OBDRequestMode.FREEZE_FRAME)))


    }

    fun start(settings: AppSettings): Observable<BaseAppAction> {
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
                            startOBDSession(it.socket, it.device, it.engine, settings)
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
                                engine: OBDEngine,
                                settings: AppSettings): Observable<BaseAppAction> {
        return Observable.concat(executeSetupCommands(engine),
                loadVehicleInfo(engine),
                loadVehicleData(engine,
                        settings))
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

    internal fun loadVehicleData(engine: OBDEngine, settings: AppSettings): Observable<BaseAppAction> {
        val dataSettings = settings.dataSettings

        val fuelLevelRequest = FuelLevelRequest(repeatable = IsRepeatable.Yes(dataSettings.fuelLevelRefreshFrequency.frequency,
                dataSettings.fuelLevelRefreshFrequency.unit))

        val temperatureRequest = OBDMultiRequest(TEMPERATURE, listOf(TemperatureRequest(TemperatureType.AIR_INTAKE),
                TemperatureRequest(TemperatureType.AMBIENT_AIR)),
                IsRepeatable.Yes(dataSettings.temperatureRefreshFrequency.frequency, dataSettings.temperatureRefreshFrequency.unit))


        val fastChangingDataRequest = OBDMultiRequest(FAST_CHANGING_DATA,
                listOf(SpeedRequest(),
                        RPMRequest(),
                        IgnitionMonitorRequest()),
                IsRepeatable.Yes(dataSettings.fastChangingDataRefreshFrequency.frequency,
                        dataSettings.fastChangingDataRefreshFrequency.unit))




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
                                                .onErrorReturn {
                                                    MILStatus.On(listOf(), UnAvailableAvailableData.UnAvailable)
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

    internal fun clearDTCs(engine: OBDEngine): Observable<BaseAppAction> {
        return engine.submit<OBDResponse>(ResetTroubleCodesCommand())
                .flatMap {
                    Observable.fromArray<BaseAppAction>(BaseAppAction.AddMilStatus(MILStatus.Off),
                            BaseAppAction.UpdateClearDTCsOperationStateToSuccessful)
                }
                .onErrorReturn { BaseAppAction.UpdateClearDTCsOperationStateToFailed(ClearDTCError.UnkownError(it)) }
                .startWith(BaseAppAction.UpdateClearDTCsOperationStateToClearing)
    }

}

