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

    private val pipe: IOBDRequestPipe by lazy {
        if (sourceInputStream.isSimulationEnabled()) {
            OBDRequestPipe(ioScheduler, computationScheduler,
                    OBDDevice(sourceInputStream, sourceOutputStream))
        } else {
            SimulatorRequestPipe()
        }
    }

    fun <T : OBDResponse> submit(request: OBDRequest): Observable<T> {
        return pipe.submitRequest(request)
    }

}




