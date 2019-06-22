package com.exp.carconnect.obdlib

import com.exp.carconnect.obdlib.obdmessage.OBDRequest
import com.exp.carconnect.obdlib.obdmessage.OBDResponse
import io.reactivex.Observable


internal interface Executable {
    @Throws(ExecutionException::class)
    fun execute(device: IOBDDevice,
                responseHandler: (OBDRequest, String) -> OBDResponse = this::toResponse): Observable<OBDResponse>

    fun toResponse(request: OBDRequest, rawResponse: String): OBDResponse
}

class ExecutionException(val request: OBDRequest, exception: Throwable) : RuntimeException(exception)
