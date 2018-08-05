package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import com.exp.carconnect.base.network.VehicleInfoLoader
import com.exp.carconnect.base.state.*
import com.exp.carconnect.obdlib.FailedOBDResponse
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.OBDMultiRequest
import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import timber.log.Timber
import java.util.concurrent.TimeUnit

class OBDDeviceSessionManager(private val context: Context,
                              private val ioScheduler: Scheduler,
                              private val computationScheduler: Scheduler,
                              private val mainScheduler: Scheduler,
                              private val vehicleInfoLoader: VehicleInfoLoader) {
    companion object {
        const val TAG = "OBDDeviceSessionManager"
    }

    private var activeSession: OBDSession? = null

    fun startNewSession(device: BluetoothDevice,
                        settings: AppSettings): Observable<BaseAppAction> {
        activeSession = OBDSession(device,
                ioScheduler,
                computationScheduler,
                mainScheduler,
                vehicleInfoLoader)

        return activeSession!!
                .start(settings)
                .doOnDispose { Timber.d("[$TAG] Active session data loading stopped") }
    }

    fun killActiveSession() {
        Timber.d("[$TAG] Killing an active session")
        activeSession = null
    }

    fun updateDataLoadFrequencyForActiveSession(engine: OBDEngine,
                                                settings: AppSettings): Observable<BaseAppAction> {
        return activeSession
                ?.loadVehicleData(engine, settings)
                ?.doOnDispose { Timber.d("[$TAG] Active session data loading stopped") }
                ?: Observable.empty()
    }

    fun clearDTCs(engine: OBDEngine): Observable<BaseAppAction> {
        return activeSession
                ?.clearDTCs(engine)
                ?: Observable.empty()
    }

    fun fetchReport(engine: OBDEngine, availablePIDs: Set<String>): Observable<BaseAppAction> {
        return activeSession?.fetchReport(engine, availablePIDs) ?: Observable.empty()
    }

    fun startBackgroundSession() {
        context.startService(Intent(context, SessionForegroundService::class.java))

    }

    fun stopBackgroundSession() {
        context.stopService(Intent(context, SessionForegroundService::class.java))
    }


}

//todo handle FailedOBDResponse for all the multi request
class OBDSession(val device: BluetoothDevice,
                 private val ioScheduler: Scheduler,
                 private val computationScheduler: Scheduler,
                 private val mainScheduler: Scheduler,
                 private val vehicleInfoLoader: VehicleInfoLoader) {

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
                OBDStandardRequest(),
                AvailablePidsCommand(PidCommand.ONE_TO_TWENTY),
                AvailablePidsCommand(PidCommand.TWENTY_ONE_TO_FOURTY),
                AvailablePidsCommand(PidCommand.FOURTY_ONE_TO_SIXTY)))

        private val dtcNumberRequest = DTCNumberRequest(repeatable = IsRepeatable.Yes(1, TimeUnit.MINUTES))
        private val pendingTroubleCodesRequest = PendingTroubleCodesRequest(TroubleCodeCommandType.ALL)
        private val freezeFrameRequest = OBDMultiRequest(FREEZE_FRAME,
                listOf(SpeedRequest(mode = OBDRequestMode.FREEZE_FRAME),
                        RPMRequest(mode = OBDRequestMode.FREEZE_FRAME)))

        private val reportRequests = listOf<OBDRequest>(LoadRequest(),
                FuelPressureRequest(),
                IntakeManifoldPressureRequest(),
                TimingAdvanceRequest(),
                MassAirFlowRequest(),
                RuntimeRequest(RuntimeType.SINCE_ENGINE_START),
                RuntimeRequest(RuntimeType.WITH_MIL_ON),
                RuntimeRequest(RuntimeType.SINCE_DTC_CLEARED),
                DistanceRequest(distanceType = DistanceCommandType.SINCE_MIL_ON),
                FuelRailPressureRequest(),
                RelativeFuelRailPressureRequest(),
                AbsoluteFuelRailPressureRequest(),
                DistanceRequest(distanceType = DistanceCommandType.SINCE_CC_CLEARED),
                BarometricPressureRequest(),
                WidebandAirFuelRatioRequest(),
                ModuleVoltageRequest(),
                AbsoluteLoadRequest(),
                EquivalentRatioRequest(),
                OilTempRequest(),
                TemperatureRequest(TemperatureType.ENGINE_COOLANT),
                ThrottleRequest(type = ThrottleRequestType.RELATIVE_POSITION),
                ThrottleRequest(type = ThrottleRequestType.ABSOLUTE_POSITION_B),
                ThrottleRequest(type = ThrottleRequestType.ABSOLUTE_POSITION_C),
                ThrottleRequest(type = ThrottleRequestType.ACCELERATOR_PEDAL_POSITION_D),
                ThrottleRequest(type = ThrottleRequestType.ACCELERATOR_PEDAL_POSITION_E),
                ThrottleRequest(type = ThrottleRequestType.ACCELERATOR_PEDAL_POSITION_F),
                ThrottleRequest(type = ThrottleRequestType.RELATIVE_ACCELERATOR_PEDAL_POSITION),
                ThrottleRequest(type = ThrottleRequestType.COMMANDED_THROTTLE_ACTUATOR),
                CommandedEGRRequest(),
                CommandedEGRErrorRequest(),
                CommandedEvaporativePurgeRequest(),
                FuelTrimRequest(fuelTrim = FuelTrim.SHORT_TERM_BANK_1),
                FuelTrimRequest(fuelTrim = FuelTrim.SHORT_TERM_BANK_2),
                FuelTrimRequest(fuelTrim = FuelTrim.LONG_TERM_BANK_1),
                FuelTrimRequest(fuelTrim = FuelTrim.LONG_TERM_BANK_2),
                EthanolFuelPercentRequest(),
                FuelInjectionTimingRequest(),
                AbsoluteEvapSystemPressureRequest(),
                EvapSystemPressureRequest(),
                WarmupsSinceCodeClearedRequest())


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
                    Timber.d("[${OBDDeviceSessionManager.TAG}] Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
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
                .doOnNext { Timber.d("[${OBDDeviceSessionManager.TAG}] Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]") }
                .toList()
                .map { responses ->
                    var vin: String? = null
                    var fuelType: FuelType? = null
                    val availablePIDs = mutableSetOf<String>()
                    var standard: OBDStandard = OBDStandard.UNKNOWN
                    responses.forEach {
                        when (it) {
                            is VinResponse -> {
                                vin = it.vin
                            }
                            is FuelTypeResponse -> {
                                fuelType = FuelType.fromValue(it.fuelType)
                            }
                            is OBDStandardResponse -> {
                                standard = it.standard
                            }
                            is AvailablePidsResponse -> {
                                Timber.d("[${OBDDeviceSessionManager.TAG}] Available pids response ${it.availablePids}")
                                availablePIDs.addAll(it.availablePids)
                            }
                        }
                    }

                    BaseAppAction.AddVehicleInfoToActiveSession(device,
                            Vehicle(vin!!, UnAvailableAvailableData.Available(availablePIDs),
                                    UnAvailableAvailableData.Available(fuelType!!),
                                    UnAvailableAvailableData.Available(standard))) as BaseAppAction
                }
                .flatMap<BaseAppAction> {
                    val addVehicleAction = it as BaseAppAction.AddVehicleInfoToActiveSession
                    vehicleInfoLoader
                            .loadVehicleInfo(it.info.vin)
                            .map { vehicleInfo ->
                                addVehicleAction.copy(info = addVehicleAction.info.copy(attributes =
                                UnAvailableAvailableData.Available(VehicleAttributes(vehicleInfo.make, vehicleInfo.model,
                                        vehicleInfo.manufacturer, vehicleInfo.modelYear))))
                            }
                            .onErrorReturn { addVehicleAction }
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
                        IgnitionMonitorRequest(),
                        ConsumptionRateRequest(),
                        ThrottlePositionRequest()),
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
                                is ThrottlePositionResponse -> BaseAppAction.AddThrottlePosition(response.throttle)
                                is ConsumptionRateResponse -> BaseAppAction.AddFuelConsumptionRate(response.fuelRate)
                            //could be received if one the request in multi request failed
                                is FailedOBDResponse -> BaseAppAction.AddFailedOBDResponseError(response.exception)
                                else -> BaseAppAction.AddIgnition((response as IgnitionMonitorResponse).ignitionOn)
                            }
                        })
                .mergeWith(
                        engine.submit<DTCNumberResponse>(dtcNumberRequest)
                                .flatMap { dtcResponse ->
                                    if (dtcResponse.milOn) {
                                        engine
                                                .submit<PendingTroubleCodesResponse>(pendingTroubleCodesRequest)
                                                /**
                                                 *https://en.wikipedia.org/wiki/OBD-II_PIDs#Mode_02
                                                 * Current freeze frame implementation is not current, freeze frames return dtc codes which cases a particular frame to froze
                                                 * .zipWith(engine.submit<OBDResponse>(freezeFrameRequest)
                                                .toList()
                                                .map<UnAvailableAvailableData<FreezeFrame>> {
                                                var rpm = 0
                                                var speed = 0
                                                for (response in it) {
                                                if (response is SpeedResponse) {
                                                speed = response.metricSpeed
                                                } else if (response is RPMResponse) {
                                                rpm = response.rpm
                                                }
                                                }

                                                UnAvailableAvailableData.Available(FreezeFrame(rpm, speed))
                                                }
                                                .onErrorReturn { UnAvailableAvailableData.UnAvailable }.toObservable()
                                                , BiFunction<PendingTroubleCodesResponse, UnAvailableAvailableData<FreezeFrame>,
                                                Pair<PendingTroubleCodesResponse, UnAvailableAvailableData<FreezeFrame>>> { t1, t2 ->
                                                Pair(t1, t2)
                                                })**/
                                                .map {
                                                    Pair(MILStatus.On(it.codes, UnAvailableAvailableData.UnAvailable), dtcResponse.tests)
                                                }
                                                .onErrorReturn {
                                                    Pair(MILStatus.On(listOf(), UnAvailableAvailableData.UnAvailable), dtcResponse.tests)
                                                }
                                    } else {
                                        Observable.just(Pair(MILStatus.Off, dtcResponse.tests))
                                    }
                                }.map {
                                    BaseAppAction.AddMilStatusAndTests(it.first, it.second)
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
                .onErrorReturn {
                    BaseAppAction.UpdateClearDTCsOperationStateToFailed(ClearDTCError.UnkownError(it))
                }
                .startWith(BaseAppAction.UpdateClearDTCsOperationStateToClearing)
    }


    internal fun fetchReport(engine: OBDEngine, availablePIDs: Set<String>): Observable<BaseAppAction> {

        val filteredRequests = reportRequests.filter {
            availablePIDs.contains(it.command.removePrefix("01 ").trim().toLowerCase())
        }

        return engine.submit<OBDResponse>(OBDMultiRequest("Report", filteredRequests))
                .map<BaseAppAction> { response ->
                    when (response) {
                        is LoadResponse -> {
                            BaseAppAction.AddEngineLoadToReport(response.load)
                        }
                        is FuelPressureResponse -> {
                            BaseAppAction.AddFuelPressureToReport(response.fuelPressure)
                        }
                        is IntakeManifoldPressureResponse -> {
                            BaseAppAction.AddIntakeManifoldPressureToReport(response.intakeManifoldPressure)
                        }
                        is TimingAdvanceResponse -> {
                            BaseAppAction.AddTimingAdvanceToReport(response.timingAdvance)
                        }
                        is MassAirFlowResponse -> {
                            BaseAppAction.AddMassAirFlowToReport(response.maf)
                        }
                        is RuntimeResponse -> {
                            if (response.type == RuntimeType.SINCE_ENGINE_START) {
                                BaseAppAction.AddRuntimeSinceEngineStartToReport(response.value)
                            } else if (response.type == RuntimeType.WITH_MIL_ON) {
                                BaseAppAction.AddRuntimeWithMILOnToReport(response.value)
                            } else {
                                BaseAppAction.AddRuntimeSinceDTCClearedToReport(response.value)
                            }
                        }
                        is DistanceResponse -> {
                            if (response.distanceType == DistanceCommandType.SINCE_MIL_ON) {
                                BaseAppAction.AddDistanceTraveledSinceMILOnToReport(response.km)
                            } else {
                                BaseAppAction.AddDistanceSinceDTCClearedToReport(response.km)
                            }
                        }
                        is FuelRailPressureResponse -> {
                            BaseAppAction.AddFuelRailPressureToReport(response.fuelRailPressure)
                        }
                        is RelativeFuelRailPressureResponse -> {
                            BaseAppAction.AddRelativeFuelRailPressureToReport(response.relativeFuelRailPressure)
                        }

                        is AbsoluteFuelRailPressureResponse -> {
                            BaseAppAction.AddAbsoluteFuelRailPressureToReport(response.pressure)
                        }
                        is BarometricPressureResponse -> {
                            BaseAppAction.AddBarometricPressureToReport(response.barometricPressure)
                        }
                        is WidebandAirFuelRatioResponse -> {
                            BaseAppAction.AddWideBandAirFuelRatioToReport(response.wafr)
                        }
                        is ModuleVoltageResponse -> {
                            BaseAppAction.AddModuleVoltageToReport(response.voltage)
                        }
                        is AbsoluteLoadResponse -> {
                            BaseAppAction.AddAbsoluteLoadToReport(response.ratio)
                        }
                        is EquivalentRatioResponse -> {
                            BaseAppAction.AddFuelAirCommandedEquivalenceRatioToReport(response.ratio)
                        }
                        is TemperatureResponse -> {
                            BaseAppAction.AddEngineCoolantTemperatureToReport(response.temperature)

                        }

                        is ThrottleResponse -> {
                            when (response.type) {
                                ThrottleRequestType.RELATIVE_POSITION -> {
                                    BaseAppAction.AddRelativeThrottlePositionToReport(response.response)
                                }
                                ThrottleRequestType.ABSOLUTE_POSITION_B -> {
                                    BaseAppAction.AddAbsoluteThrottlePositionBToReport(response.response)
                                }
                                ThrottleRequestType.ABSOLUTE_POSITION_C -> {
                                    BaseAppAction.AddAbsoluteThrottlePositionCToReport(response.response)
                                }
                                ThrottleRequestType.ACCELERATOR_PEDAL_POSITION_D -> {
                                    BaseAppAction.AddAccelPedalPositionDToReport(response.response)
                                }
                                ThrottleRequestType.ACCELERATOR_PEDAL_POSITION_E -> {
                                    BaseAppAction.AddAccelPedalPositionEToReport(response.response)
                                }
                                ThrottleRequestType.ACCELERATOR_PEDAL_POSITION_F -> {
                                    BaseAppAction.AddAccelPedalPositionFToReport(response.response)
                                }
                                ThrottleRequestType.RELATIVE_ACCELERATOR_PEDAL_POSITION -> {
                                    BaseAppAction.AddRelativeAccelPedalPositionToReport(response.response)
                                }
                                else -> {
                                    BaseAppAction.AddCommandedThrottleActuatorToReport(response.response)
                                }
                            }
                        }
                        is CommandedEGRResponse -> {
                            BaseAppAction.AddCommandedEGRToReport(response.ratio)
                        }

                        is CommandedEGRErrorResponse -> {
                            BaseAppAction.AddEGRErrorToReport(response.error)
                        }
                        is CommandedEvaporativePurgeResponse -> {
                            BaseAppAction.AddCommandedEvaporativePurgeToReport(response.ratio)
                        }
                        is FuelTrimResponse -> {
                            when (response.type) {
                                FuelTrim.SHORT_TERM_BANK_1 -> {
                                    BaseAppAction.AddFuelTrimShortTermBank1ToReport(response.fuelTrim)
                                }
                                FuelTrim.SHORT_TERM_BANK_2 -> {
                                    BaseAppAction.AddFuelTrimShortTermBank2ToReport(response.fuelTrim)
                                }
                                FuelTrim.LONG_TERM_BANK_1 -> {
                                    BaseAppAction.AddFuelTrimLongTermBank1ToReport(response.fuelTrim)
                                }

                                else -> {
                                    BaseAppAction.AddFuelTrimLongTermBank2ToReport(response.fuelTrim)
                                }
                            }
                        }

                        is EthanolFuelPercentResponse -> {
                            BaseAppAction.AddEthanolFuelPercentageToReport(response.percent)
                        }
                        is FuelInjectionTimingResponse -> {
                            BaseAppAction.AddFuelInjectionTimingToReport(response.response)
                        }
                        is AbsoluteEvapSystemPressureResponse -> {
                            BaseAppAction.AddAbsoluteEvapSystemVaporPressureToReport(response.pressure)
                        }
                        is EvapSystemPressureResponse -> {
                            BaseAppAction.AddEvapSystemVaporPressureToReport(response.pressure)
                        }

                        is WarmupsSinceCodeClearedResponse -> {
                            BaseAppAction.AddWarmupsSinceCodeClearedToReport(response.warmUps)
                        }
                        else -> {
                            BaseAppAction.AddOilTemperatureToReport((response as OilTempResponse).temperature)
                        }
                    }


                }
                .onErrorReturn { BaseAppAction.AddFailedToLoadReportErrorToState(device, it) }
    }

}

