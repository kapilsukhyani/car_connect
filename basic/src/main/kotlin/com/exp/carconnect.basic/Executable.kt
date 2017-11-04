package com.exp.carconnect

import com.exp.carconnect.basic.obdmessage.OBDRequest
import com.exp.carconnect.basic.obdmessage.OBDResponse
import io.reactivex.Observable


internal interface Executable {
    @Throws(ExecutionException::class)
    fun execute(device: IOBDDevice): Observable<OBDResponse>
}

class ExecutionException(val request: OBDRequest, exception: Throwable) : RuntimeException(exception)
