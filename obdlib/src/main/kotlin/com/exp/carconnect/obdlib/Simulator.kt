package com.exp.carconnect.obdlib

import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Observable
import java.io.InputStream
import java.io.OutputStream

private class Input(val streams: SimulatedStreams) : InputStream() {
    override fun read(): Int {
        throw NotImplementedError()
    }
}

private class Output(val streams: SimulatedStreams) : OutputStream() {
    override fun write(b: Int) {
        throw NotImplementedError()
    }
}

fun InputStream.isSimulationEnabled(): Boolean = this is Input

fun InputStream.getSource(): SimulationResponseSource {
    return (this as Input).streams.source
}

class SimulatedStreams(val source: SimulationResponseSource = SimulationResponseSource()) {
    val inputStream: InputStream = Input(this)
    val outputStream: OutputStream = Output(this)
}

internal class SimulatedRequest(val request: OBDRequest,
                                private val source: SimulationResponseSource,
                                private val simulatedDevice: SimulatorOBDDevice) :
        OBDRequest(request.tag,
                request.command,
                request.retriable,
                request.isRepeatable,
                request.returnCachedResponse) {

    override fun execute(device: IOBDDevice,
                         responseHandler: (OBDRequest, String) -> OBDResponse): Observable<OBDResponse> {
        return request.execute(simulatedDevice, this::toResponse)
    }

    override fun toResponse(request: OBDRequest, rawResponse: String): OBDResponse {
        return source.typeToProducer[request::class.java]?.invoke(request)
                ?: {
                    try {
                        val response = super.toResponse(rawResponse)
                        if (response is RawResponse) {
                            response
                        } else {
                            throw IllegalStateException("Simulated Response Not Available [${request::javaClass.name}]")
                        }
                    } catch (e: Throwable) {
                        throw IllegalStateException("Simulated Response Not Available [${request::javaClass.name}]")
                    }
                }()
    }

}

internal class SimulatorOBDDevice : IOBDDevice {

    override fun run(request: OBDRequest): Observable<String> {
        return Observable.just("Simulated Response")
    }

    override fun getLatestResponse(command: String): Observable<String?> {
        return Observable.just(null)
    }

    override fun purgeCachedResponseFor(command: String) {
    }

    override fun purgeAllCachedResponses() {
    }
}


class SimulationResponseSource(freezeDTCResponseProducer: (FreezeDTCRequest) -> FreezeDTCResponse = defaultFreezeDTCResponseProducer,
                               distanceResponseProducer: (DistanceRequest) -> DistanceResponse = defaultDistanceResponseProducer,
                               dtcNumberResponseProducer: (DTCNumberRequest) -> DTCNumberResponse = defaultDTCNumberResponseProducer,
                               equivalentRatioResponseProducer: (EquivalentRatioRequest) -> EquivalentRatioResponse = defaultEquivalentRatioResponseProducer,
                               ignitionMonitorResponseProducer: (IgnitionMonitorRequest) -> IgnitionMonitorResponse = defaultIgnitionMonitorResponseProducer,
                               moduleVoltageResponseProducer: (ModuleVoltageRequest) -> ModuleVoltageResponse = defaultModuleVoltageResponseProducer,
                               pendingTroubleCodesResponseProducer: (PendingTroubleCodesRequest) -> PendingTroubleCodesResponse = defaultPendingTroubleCodesResponseProducer,
                               timingAdvanceResponseProducer: (TimingAdvanceRequest) -> TimingAdvanceResponse = defaultTimingAdvanceResponseProducer,
                               vinResponseProducer: (VinRequest) -> VinResponse = defaultVinResponseProducer,

                               rpmResponseProducer: (RPMRequest) -> RPMResponse = defaultRPMResponseProducer,
                               absoluteLoadResponseProducer: (AbsoluteLoadRequest) -> AbsoluteLoadResponse = defaultAbsoluteLoadResponseProducer,
                               commandedEGRResponseProducer: (CommandedEGRRequest) -> CommandedEGRResponse = defaultCommandedEGRResponseProducer,
                               commandedEGRErrorResponseProducer: (CommandedEGRErrorRequest) -> CommandedEGRErrorResponse = defaultCommandedEGRErrorResponseProducer,
                               commandedEvaporativePurgeResponseProducer: (CommandedEvaporativePurgeRequest) -> CommandedEvaporativePurgeResponse = defaultCommandedEvaporativePurgeResponseProducer,
                               warmupsSinceCodeClearedResponseProducer: (WarmupsSinceCodeClearedRequest) -> WarmupsSinceCodeClearedResponse = defaultWarmupsSinceCodeClearedResponseProducer,
                               loadResponseProducer: (LoadRequest) -> LoadResponse = defaultLoadResponseProducer,
                               massAirFlowResponseProducer: (MassAirFlowRequest) -> MassAirFlowResponse = defaultMassAirFlowResponseProducer,
                               oilTempResponseProducer: (OilTempRequest) -> OilTempResponse = defaultOilTempResponseProducer,
                               runtimeResponseProducer: (RuntimeRequest) -> RuntimeResponse = defaultRuntimeResponseProducer,
                               throttlePositionResponseProducer: (ThrottlePositionRequest) -> ThrottlePositionResponse = defaultThrottlePositionResponseProducer,
                               throttleResponseProducer: (ThrottleRequest) -> ThrottleResponse = defaultThrottleResponseProducer,
                               speedResponseProducer: (SpeedRequest) -> SpeedResponse = defaultSpeedResponseProducer,

                               airFuelRatioResponseProducer: (AirFuelRatioRequest) -> AirFuelRatioResponse = defaultAirFuelRatioResponseProducer,
                               consumptionRateResponseProducer: (ConsumptionRateRequest) -> ConsumptionRateResponse = defaultConsumptionRateResponseProducer,
                               fuelTypeResponseProducer: (FuelTypeRequest) -> FuelTypeResponse = defaultFuelTypeResponseProducer,
                               fuelLevelResponseProducer: (FuelLevelRequest) -> FuelLevelResponse = defaultFuelLevelResponseProducer,
                               fuelTrimResponseProducer: (FuelTrimRequest) -> FuelTrimResponse = defaultFuelTrimResponseProducer,
                               widebandAirFuelRatioResponseProducer: (WidebandAirFuelRatioRequest) -> WidebandAirFuelRatioResponse = defaultWidebandAirFuelRatioResponseProducer,
                               ethanolFuelPercentResponseProducer: (EthanolFuelPercentRequest) -> EthanolFuelPercentResponse = defaultEthanolFuelPercentResponseProducer,
                               fuelInjectionTimingResponseProducer: (FuelInjectionTimingRequest) -> FuelInjectionTimingResponse = defaultFuelInjectionTimingResponseProducer,

                               barometricPressureResponseProducer: (BarometricPressureRequest) -> BarometricPressureResponse = defaultBarometricPressureResponseProducer,
                               fuelPressureResponseProducer: (FuelPressureRequest) -> FuelPressureResponse = defaultFuelPressureResponseProducer,
                               fuelRailPressureResponseProducer: (FuelRailPressureRequest) -> FuelRailPressureResponse = defaultFuelRailPressureResponseProducer,
                               relativeFuelRailPressureResponseProducer: (RelativeFuelRailPressureRequest) -> RelativeFuelRailPressureResponse = defaultRelativeFuelRailPressureResponseProducer,
                               absoluteFuelRailPressureResponseProducer: (AbsoluteFuelRailPressureRequest) -> AbsoluteFuelRailPressureResponse = defaultAbsoluteFuelRailPressureResponseProducer,
                               intakeManifoldPressureResponseProducer: (IntakeManifoldPressureRequest) -> IntakeManifoldPressureResponse = defaultIntakeManifoldPressureResponseProducer,
                               absoluteEvapSystemPressureResponseProducer: (AbsoluteEvapSystemPressureRequest) -> AbsoluteEvapSystemPressureResponse = defaultAbsoluteEvapSystemPressureResponseProducer,
                               evapSystemPressureResponseProducer: (EvapSystemPressureRequest) -> EvapSystemPressureResponse = defaultEvapSystemPressureResponseProducer,

                               availablePidsResponseProducer: (AvailablePidsCommand) -> AvailablePidsResponse = defaultAvailablePidsResponseProducer,
                               obdStandardResponseProducer: (OBDStandardRequest) -> OBDStandardResponse = defaultOBDStandardResponseProducer,

                               temperatureResponseProducer: (TemperatureRequest) -> TemperatureResponse = defaultTemperatureResponseProducer,
                               catalystTemperatureResponseProducer: (CatalystTemperatureRequest) -> CatalystTemperatureResponse = defaultCatalystTemperatureResponseProducer) {


    internal val typeToProducer = mutableMapOf<Class<out OBDRequest>, (OBDRequest) -> OBDResponse>()

    companion object {
        val defaultFreezeDTCResponseProducer: (FreezeDTCRequest) -> FreezeDTCResponse = {
            FreezeDTCResponse(false)
        }

        val defaultDistanceResponseProducer: (DistanceRequest) -> DistanceResponse = {
            DistanceResponse(it.distanceType, 100)
        }

        val defaultDTCNumberResponseProducer: (DTCNumberRequest) -> DTCNumberResponse = {
            DTCNumberResponse(1,
                    true,
                    MotorType.COMPRESSION,
                    arrayOf(MonitorTest(TestType.COMPONENTS,
                            available = true,
                            complete = true)))
        }

        val defaultEquivalentRatioResponseProducer: (EquivalentRatioRequest) -> EquivalentRatioResponse = {
            EquivalentRatioResponse(2.1f)
        }

        val defaultIgnitionMonitorResponseProducer: (IgnitionMonitorRequest) -> IgnitionMonitorResponse = {
            IgnitionMonitorResponse(true)
        }

        val defaultModuleVoltageResponseProducer: (ModuleVoltageRequest) -> ModuleVoltageResponse = {
            ModuleVoltageResponse(34.3f)
        }

        val defaultPendingTroubleCodesResponseProducer: (PendingTroubleCodesRequest) -> PendingTroubleCodesResponse = {
            PendingTroubleCodesResponse(listOf("P1021", "P1002"))
        }
        val defaultTimingAdvanceResponseProducer: (TimingAdvanceRequest) -> TimingAdvanceResponse = {
            TimingAdvanceResponse(23.0f)
        }
        val defaultVinResponseProducer: (VinRequest) -> VinResponse = {
            //random vin generator https://vingenerator.org/
            VinResponse(vin = "WP0AA2A79BL017244")
        }


        val defaultRPMResponseProducer: (RPMRequest) -> RPMResponse = {
            RPMResponse(3000)
        }
        val defaultAbsoluteLoadResponseProducer: (AbsoluteLoadRequest) -> AbsoluteLoadResponse = {
            AbsoluteLoadResponse(34.9f)
        }
        val defaultCommandedEGRResponseProducer: (CommandedEGRRequest) -> CommandedEGRResponse = {
            CommandedEGRResponse(14.9f)
        }
        val defaultCommandedEGRErrorResponseProducer: (CommandedEGRErrorRequest) -> CommandedEGRErrorResponse = {
            CommandedEGRErrorResponse(10.9f)
        }
        val defaultCommandedEvaporativePurgeResponseProducer: (CommandedEvaporativePurgeRequest) -> CommandedEvaporativePurgeResponse = {
            CommandedEvaporativePurgeResponse(20.9f)
        }
        val defaultWarmupsSinceCodeClearedResponseProducer: (WarmupsSinceCodeClearedRequest) -> WarmupsSinceCodeClearedResponse = {
            WarmupsSinceCodeClearedResponse(3)
        }
        val defaultLoadResponseProducer: (LoadRequest) -> LoadResponse = {
            LoadResponse(3.3f)
        }
        val defaultMassAirFlowResponseProducer: (MassAirFlowRequest) -> MassAirFlowResponse = {
            MassAirFlowResponse(32.3f)
        }
        val defaultOilTempResponseProducer: (OilTempRequest) -> OilTempResponse = {
            OilTempResponse(21)
        }
        val defaultRuntimeResponseProducer: (RuntimeRequest) -> RuntimeResponse = {
            RuntimeResponse(5000, it.type)
        }
        val defaultThrottlePositionResponseProducer: (ThrottlePositionRequest) -> ThrottlePositionResponse = {
            ThrottlePositionResponse(1.2f)
        }
        val defaultThrottleResponseProducer: (ThrottleRequest) -> ThrottleResponse = {
            ThrottleResponse(1.2f, it.type)
        }
        val defaultSpeedResponseProducer: (SpeedRequest) -> SpeedResponse = {
            SpeedResponse(100)
        }

        val defaultAirFuelRatioResponseProducer: (AirFuelRatioRequest) -> AirFuelRatioResponse = {
            AirFuelRatioResponse(10.0f)
        }
        val defaultConsumptionRateResponseProducer: (ConsumptionRateRequest) -> ConsumptionRateResponse = {
            ConsumptionRateResponse(10.0f)
        }
        val defaultFuelTypeResponseProducer: (FuelTypeRequest) -> FuelTypeResponse = {
            FuelTypeResponse(1)
        }
        val defaultFuelLevelResponseProducer: (FuelLevelRequest) -> FuelLevelResponse = {
            FuelLevelResponse(32.2f)
        }
        val defaultFuelTrimResponseProducer: (FuelTrimRequest) -> FuelTrimResponse = {
            FuelTrimResponse(32.2f, it.fuelTrim)
        }
        val defaultWidebandAirFuelRatioResponseProducer: (WidebandAirFuelRatioRequest) -> WidebandAirFuelRatioResponse = {
            WidebandAirFuelRatioResponse(32.2f)
        }
        val defaultEthanolFuelPercentResponseProducer: (EthanolFuelPercentRequest) -> EthanolFuelPercentResponse = {
            EthanolFuelPercentResponse(31.2f)
        }
        val defaultFuelInjectionTimingResponseProducer: (FuelInjectionTimingRequest) -> FuelInjectionTimingResponse = {
            FuelInjectionTimingResponse(11.2f)
        }


        val defaultBarometricPressureResponseProducer: (BarometricPressureRequest) -> BarometricPressureResponse = {
            BarometricPressureResponse(11)
        }
        val defaultFuelPressureResponseProducer: (FuelPressureRequest) -> FuelPressureResponse = {
            FuelPressureResponse(31)
        }
        val defaultFuelRailPressureResponseProducer: (FuelRailPressureRequest) -> FuelRailPressureResponse = {
            FuelRailPressureResponse(21)
        }
        val defaultRelativeFuelRailPressureResponseProducer: (RelativeFuelRailPressureRequest) -> RelativeFuelRailPressureResponse = {
            RelativeFuelRailPressureResponse(11f)
        }
        val defaultAbsoluteFuelRailPressureResponseProducer: (AbsoluteFuelRailPressureRequest) -> AbsoluteFuelRailPressureResponse = {
            AbsoluteFuelRailPressureResponse(19f)
        }
        val defaultIntakeManifoldPressureResponseProducer: (IntakeManifoldPressureRequest) -> IntakeManifoldPressureResponse = {
            IntakeManifoldPressureResponse(9)
        }
        val defaultAbsoluteEvapSystemPressureResponseProducer: (AbsoluteEvapSystemPressureRequest) -> AbsoluteEvapSystemPressureResponse = {
            AbsoluteEvapSystemPressureResponse(98f)
        }
        val defaultEvapSystemPressureResponseProducer: (EvapSystemPressureRequest) -> EvapSystemPressureResponse = {
            EvapSystemPressureResponse(18f)
        }


        val defaultAvailablePidsResponseProducer: (AvailablePidsCommand) -> AvailablePidsResponse = {
            AvailablePidsResponse(setOf("dc0012, p0102"), it.pidCommand)
        }
        val defaultOBDStandardResponseProducer: (OBDStandardRequest) -> OBDStandardResponse = {
            OBDStandardResponse(OBDStandard.CARB_OBD2)
        }

        val defaultTemperatureResponseProducer: (TemperatureRequest) -> TemperatureResponse = {
            TemperatureResponse(20, it.temperatureType)
        }
        val defaultCatalystTemperatureResponseProducer: (CatalystTemperatureRequest) -> CatalystTemperatureResponse = {
            CatalystTemperatureResponse(20.0f, it.temperatureType)
        }
    }

    init {
        typeToProducer[FreezeDTCRequest::class.java] = {
            freezeDTCResponseProducer(it as FreezeDTCRequest)
        }
        typeToProducer[DistanceRequest::class.java] = {
            distanceResponseProducer(it as DistanceRequest)
        }
        typeToProducer[DTCNumberRequest::class.java] = {
            dtcNumberResponseProducer(it as DTCNumberRequest)
        }
        typeToProducer[EquivalentRatioRequest::class.java] = {
            equivalentRatioResponseProducer(it as EquivalentRatioRequest)
        }
        typeToProducer[IgnitionMonitorRequest::class.java] = {
            ignitionMonitorResponseProducer(it as IgnitionMonitorRequest)
        }
        typeToProducer[ModuleVoltageRequest::class.java] = {
            moduleVoltageResponseProducer(it as ModuleVoltageRequest)
        }
        typeToProducer[PendingTroubleCodesRequest::class.java] = {
            pendingTroubleCodesResponseProducer(it as PendingTroubleCodesRequest)
        }
        typeToProducer[TimingAdvanceRequest::class.java] = {
            timingAdvanceResponseProducer(it as TimingAdvanceRequest)
        }
        typeToProducer[VinRequest::class.java] = {
            vinResponseProducer(it as VinRequest)
        }


        typeToProducer[RPMRequest::class.java] = {
            rpmResponseProducer(it as RPMRequest)
        }
        typeToProducer[AbsoluteLoadRequest::class.java] = {
            absoluteLoadResponseProducer(it as AbsoluteLoadRequest)
        }
        typeToProducer[CommandedEGRRequest::class.java] = {
            commandedEGRResponseProducer(it as CommandedEGRRequest)
        }
        typeToProducer[CommandedEGRErrorRequest::class.java] = {
            commandedEGRErrorResponseProducer(it as CommandedEGRErrorRequest)
        }
        typeToProducer[CommandedEvaporativePurgeRequest::class.java] = {
            commandedEvaporativePurgeResponseProducer(it as CommandedEvaporativePurgeRequest)
        }
        typeToProducer[WarmupsSinceCodeClearedRequest::class.java] = {
            warmupsSinceCodeClearedResponseProducer(it as WarmupsSinceCodeClearedRequest)
        }
        typeToProducer[LoadRequest::class.java] = {
            loadResponseProducer(it as LoadRequest)
        }
        typeToProducer[MassAirFlowRequest::class.java] = {
            massAirFlowResponseProducer(it as MassAirFlowRequest)
        }
        typeToProducer[OilTempRequest::class.java] = {
            oilTempResponseProducer(it as OilTempRequest)
        }
        typeToProducer[RuntimeRequest::class.java] = {
            runtimeResponseProducer(it as RuntimeRequest)
        }
        typeToProducer[ThrottlePositionRequest::class.java] = {
            throttlePositionResponseProducer(it as ThrottlePositionRequest)
        }
        typeToProducer[ThrottleRequest::class.java] = {
            throttleResponseProducer(it as ThrottleRequest)
        }
        typeToProducer[SpeedRequest::class.java] = {
            speedResponseProducer(it as SpeedRequest)
        }


        typeToProducer[AirFuelRatioRequest::class.java] = {
            airFuelRatioResponseProducer(it as AirFuelRatioRequest)
        }
        typeToProducer[ConsumptionRateRequest::class.java] = {
            consumptionRateResponseProducer(it as ConsumptionRateRequest)
        }
        typeToProducer[FuelTypeRequest::class.java] = {
            fuelTypeResponseProducer(it as FuelTypeRequest)
        }
        typeToProducer[FuelLevelRequest::class.java] = {
            fuelLevelResponseProducer(it as FuelLevelRequest)
        }
        typeToProducer[FuelTrimRequest::class.java] = {
            fuelTrimResponseProducer(it as FuelTrimRequest)
        }
        typeToProducer[WidebandAirFuelRatioRequest::class.java] = {
            widebandAirFuelRatioResponseProducer(it as WidebandAirFuelRatioRequest)
        }
        typeToProducer[EthanolFuelPercentRequest::class.java] = {
            ethanolFuelPercentResponseProducer(it as EthanolFuelPercentRequest)
        }
        typeToProducer[FuelInjectionTimingRequest::class.java] = {
            fuelInjectionTimingResponseProducer(it as FuelInjectionTimingRequest)
        }


        typeToProducer[BarometricPressureRequest::class.java] = {
            barometricPressureResponseProducer(it as BarometricPressureRequest)
        }
        typeToProducer[FuelPressureRequest::class.java] = {
            fuelPressureResponseProducer(it as FuelPressureRequest)
        }
        typeToProducer[FuelRailPressureRequest::class.java] = {
            fuelRailPressureResponseProducer(it as FuelRailPressureRequest)
        }
        typeToProducer[RelativeFuelRailPressureRequest::class.java] = {
            relativeFuelRailPressureResponseProducer(it as RelativeFuelRailPressureRequest)
        }
        typeToProducer[AbsoluteFuelRailPressureRequest::class.java] = {
            absoluteFuelRailPressureResponseProducer(it as AbsoluteFuelRailPressureRequest)
        }
        typeToProducer[IntakeManifoldPressureRequest::class.java] = {
            intakeManifoldPressureResponseProducer(it as IntakeManifoldPressureRequest)
        }
        typeToProducer[AbsoluteEvapSystemPressureRequest::class.java] = {
            absoluteEvapSystemPressureResponseProducer(it as AbsoluteEvapSystemPressureRequest)
        }
        typeToProducer[EvapSystemPressureRequest::class.java] = {
            evapSystemPressureResponseProducer(it as EvapSystemPressureRequest)
        }


        typeToProducer[AvailablePidsCommand::class.java] = {
            availablePidsResponseProducer(it as AvailablePidsCommand)
        }
        typeToProducer[OBDStandardRequest::class.java] = {
            obdStandardResponseProducer(it as OBDStandardRequest)
        }

        typeToProducer[TemperatureRequest::class.java] = {
            temperatureResponseProducer(it as TemperatureRequest)
        }
        typeToProducer[CatalystTemperatureRequest::class.java] = {
            catalystTemperatureResponseProducer(it as CatalystTemperatureRequest)
        }
    }

}