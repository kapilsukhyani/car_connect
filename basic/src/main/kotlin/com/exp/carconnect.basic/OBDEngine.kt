package com.exp.carconnect.basic

import com.exp.carconnect.OBDDevice
import com.exp.carconnect.OBDRequestPipe
import com.exp.carconnect.basic.obdmessage.OBDRequest
import com.exp.carconnect.basic.obdmessage.OBDResponse
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.io.InputStream
import java.io.OutputStream

class OBDEngine(sourceInputStream: InputStream,
                sourceOutputStream: OutputStream,
                ioScheduler: Scheduler,
                computationScheduler: Scheduler) {
    companion object {
        val TAG = "OBDEngine"
    }

    private val pipe = OBDRequestPipe(ioScheduler, computationScheduler,
            OBDDevice(sourceInputStream, sourceOutputStream))

    fun <T : OBDResponse> submit(request: OBDRequest): Observable<T> {
        return pipe.submitRequest(request)
    }

}




