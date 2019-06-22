package com.exp.carconnect.obdlib

import com.exp.carconnect.obdlib.obdmessage.OBDRequest
import com.exp.carconnect.obdlib.obdmessage.OBDResponse
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.io.InputStream
import java.io.OutputStream

class OBDEngine(sourceInputStream: InputStream,
                sourceOutputStream: OutputStream,
                ioScheduler: Scheduler,
                computationScheduler: Scheduler) {
    companion object {
        const val TAG = "OBDEngine"
    }

    private val simulationEnabled = sourceInputStream.isSimulationEnabled()
    private val simulationResponseSource: SimulationResponseSource by lazy {
        sourceInputStream.getSource()
    }
    private val simulatorOBDDevice = SimulatorOBDDevice()

    private val pipe: IOBDRequestPipe by lazy {
        OBDRequestPipe(ioScheduler,
                computationScheduler,
                OBDDevice(sourceInputStream, sourceOutputStream))
    }

    fun <T : OBDResponse> submit(request: OBDRequest): Observable<T> {
        val req = if (simulationEnabled) {
            SimulatedRequest(request,
                    simulationResponseSource,
                    simulatorOBDDevice)
        } else {
            request
        }
        return pipe.submitRequest(req)
    }

}




