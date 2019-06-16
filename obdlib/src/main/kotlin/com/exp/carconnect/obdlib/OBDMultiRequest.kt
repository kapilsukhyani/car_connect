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

    override fun execute(device: IOBDDevice): Observable<OBDResponse> {
        return Observable.fromIterable(requests)
                .flatMap {
                    it.execute(device)
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