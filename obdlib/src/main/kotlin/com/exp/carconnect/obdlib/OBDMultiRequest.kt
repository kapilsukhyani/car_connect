package com.exp.carconnect.obdlib

import com.exp.carconnect.obdlib.obdmessage.IsRepeatable
import com.exp.carconnect.obdlib.obdmessage.OBDRequest
import com.exp.carconnect.obdlib.obdmessage.OBDResponse
import io.reactivex.Observable
import java.util.*

open class OBDMultiRequest(tag: String,
                           requests: List<OBDRequest>,
                           repeatable: IsRepeatable = IsRepeatable.No) : OBDRequest(tag, "", true, repeatable) {
    private val requests: List<OBDRequest> = Collections.unmodifiableList(requests)

    override fun execute(device: IOBDDevice,
                         responseHandler: (OBDRequest, String) -> OBDResponse): Observable<OBDResponse> {
        return Observable.fromIterable(requests)
                .flatMap {
                    // if handler is OBDMultiRequest::toResponse then we want to override it or else we want to use injected one (simulator response handler)
                    //TODO fix this hack with a better solution
                    val selectedHandler = if (responseHandler::class.java.name == "com.exp.carconnect.obdlib.Executable\$execute$1") {
                        it::toResponse
                    } else {
                        responseHandler
                    }
                    it.execute(device, selectedHandler)
                            .onErrorReturn { error ->
                                when (error) {
                                    is ExecutionException -> getFailedOBDResponse(error)
                                    else -> throw error
                                }
                            }
                }

    }

    protected open fun getFailedOBDResponse(exception: ExecutionException): OBDResponse {
        return FailedOBDResponse(exception)
    }

}

open class FailedOBDResponse(val exception: ExecutionException) : OBDResponse("FailedResponse", "")