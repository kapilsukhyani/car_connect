package com.exp.carconnect

import com.exp.carconnect.basic.obdmessage.IsRepeatable
import com.exp.carconnect.basic.obdmessage.OBDRequest
import com.exp.carconnect.basic.obdmessage.OBDResponse
import io.reactivex.Observable
import java.util.*

open class OBDMultiRequest(tag: String,
                           requests: List<OBDRequest>,
                           repeatable: IsRepeatable = IsRepeatable.No) : OBDRequest(tag, "", true, repeatable) {
    val requests: List<OBDRequest>

    init {
        this.requests = Collections.unmodifiableList(requests)
    }

    override fun execute(device: IOBDDevice): Observable<OBDResponse> {
        return Observable.fromIterable(requests)
                .flatMap {
                    it.execute(device)
                            .onErrorReturn { it: Throwable ->
                                when (it) {
                                    is ExecutionException -> getFailedOBDResponse(it)
                                    else -> throw it
                                }
                            }
                }

    }

    protected open fun getFailedOBDResponse(exception: ExecutionException): OBDResponse {
        return FailedOBDResponse(exception)
    }

}

open class FailedOBDResponse(val exception: ExecutionException) : OBDResponse("FailedResponse", "")