package com.exp.carconnect.obdlib

import com.exp.carconnect.obdlib.obdmessage.*
import io.reactivex.Observable
import java.io.InputStream
import java.io.OutputStream

private class Input : InputStream() {
    override fun read(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

private class Output : OutputStream() {
    override fun write(b: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun InputStream.isSimulationEnabled(): Boolean = this is Input

class SimulatedStreams {
    companion object {
        val input: InputStream = Input()
        val output: OutputStream = Output()
    }

    val inputStream: InputStream = input
    val outputStream: OutputStream = output
}

internal class SimulatorRequestPipe(private val source: SimulationResponseSource = SimulationResponseSource()) : IOBDRequestPipe {

    override fun <T : OBDResponse> submitRequest(request: OBDRequest): Observable<T> {
        return source.typeToProducer[request::class.java]?.invoke(request)?.map { it as T }
                ?: Observable.error(Throwable("Simulated response not available for request[${request.command}]"))
    }

}


class SimulationResponseSource(freezeDTCResponseProducer: (FreezeDTCRequest) -> Observable<FreezeDTCResponse> = defaultFreezeDTCResponseProducer,
                               distanceResponseProducer: (DistanceRequest) -> Observable<DistanceResponse> = defaultDistanceResponseProducer,
                               dtcNumberResponseProducer: (DTCNumberRequest) -> Observable<DTCNumberResponse> = defaultDTCNumberResponseProducer,
                               equivalentRatioResponseProducer: (EquivalentRatioRequest) -> Observable<EquivalentRatioResponse> = defaultEquivalentRatioResponseProducer,
                               ignitionMonitorResponseProducer: (IgnitionMonitorRequest) -> Observable<IgnitionMonitorResponse> = defaultIgnitionMonitorResponseProducer,
                               moduleVoltageResponseProducer: (ModuleVoltageRequest) -> Observable<ModuleVoltageResponse> = defaultModuleVoltageResponseProducer,
                               pendingTroubleCodesResponseProducer: (PendingTroubleCodesRequest) -> Observable<PendingTroubleCodesResponse> = defaultPendingTroubleCodesResponseProducer,
                               timingAdvanceResponseProducer: (TimingAdvanceRequest) -> Observable<TimingAdvanceResponse> = defaultTimingAdvanceResponseProducer,
                               vinResponseProducer: (VinRequest) -> Observable<VinResponse> = defaultVinResponseProducer) {

    internal val typeToProducer = mutableMapOf<Class<out OBDRequest>, (OBDRequest) -> Observable<out OBDResponse>>()

    companion object {
        val defaultFreezeDTCResponseProducer: (FreezeDTCRequest) -> Observable<FreezeDTCResponse> = {
            Observable.just(FreezeDTCResponse("false"))
        }

        val defaultDistanceResponseProducer: (DistanceRequest) -> Observable<DistanceResponse> = {
            Observable.just(DistanceResponse(it.distanceType, "00000102"))
        }

        val defaultDTCNumberResponseProducer: (DTCNumberRequest) -> Observable<DTCNumberResponse> = {
            Observable.just(DTCNumberResponse("00000102"))
        }

        val defaultEquivalentRatioResponseProducer: (EquivalentRatioRequest) -> Observable<EquivalentRatioResponse> = {
            Observable.just(EquivalentRatioResponse(""))
        }

        val defaultIgnitionMonitorResponseProducer: (IgnitionMonitorRequest) -> Observable<IgnitionMonitorResponse> = {
            Observable.just(IgnitionMonitorResponse("ON"))
        }

        val defaultModuleVoltageResponseProducer: (ModuleVoltageRequest) -> Observable<ModuleVoltageResponse> = {
            Observable.just(ModuleVoltageResponse("00000A0B"))
        }

        val defaultPendingTroubleCodesResponseProducer: (PendingTroubleCodesRequest) -> Observable<PendingTroubleCodesResponse> = {
            Observable.just(PendingTroubleCodesResponse("00000A0B"))
        }
        val defaultTimingAdvanceResponseProducer: (TimingAdvanceRequest) -> Observable<TimingAdvanceResponse> = {
            Observable.just(TimingAdvanceResponse("00000A0B"))
        }
        val defaultVinResponseProducer: (VinRequest) -> Observable<VinResponse> = {
            Observable.just(VinResponse("00000A0B"))
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

    }

}