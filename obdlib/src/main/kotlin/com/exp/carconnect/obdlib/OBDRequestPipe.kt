package com.exp.carconnect.obdlib

import com.exp.carconnect.obdlib.obdmessage.IsRepeatable
import com.exp.carconnect.obdlib.obdmessage.OBDRequest
import com.exp.carconnect.obdlib.obdmessage.OBDResponse
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import java.util.*


internal class OBDRequestPipe(private val ioScheduler: Scheduler,
                              private val computationScheduler: Scheduler,
                              private val obdDevice: IOBDDevice) : IOBDRequestPipe {

    companion object {
        const val TAG = "OBDRequestPipe"
    }

    override fun <T : OBDResponse> submitRequest(request: OBDRequest): Observable<T> {
        return Observable
                .create<OBDResponse> { emitter ->
                    obdRequestPipe.onNext(Item(request, emitter))
                }
                .map {
                    // this is just a small trick to return strongly typed values instead of letting callers typecast each time
                    it as T
                }
                .repeatWhen { completionObservable ->
                    when (request.isRepeatable) {
                        is IsRepeatable.No -> completionObservable.take(1)
                        is IsRepeatable.Yes -> completionObservable.delay(request.isRepeatable.frequency, request.isRepeatable.unit)
                    }
                }
                //this will free the request processing thread to proceed with another request
                .observeOn(computationScheduler)
    }


    private data class Item(val request: OBDRequest,
                            val responseEmitter: ObservableEmitter<OBDResponse>)

    private val obdRequestPipe = PublishSubject.create<Item>()

    init {
        init()
    }

    private fun init() {
        val disposable = obdRequestPipe
                .serialize()
                .toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(ioScheduler)
                .subscribe(
                        { item ->
                            OBDLogger.log(TAG, "Submitted [${item.request.tag}] at [${Date()}]")
                            executeRequestIfObserverAlive(item)
                        },
                        { error ->
                            OBDLogger.log(TAG, error)
                        },
                        {
                            OBDLogger.log(TAG, "request pipe completed")
                        })

    }


    private fun executeRequestIfObserverAlive(item: Item) {
        if (item.responseEmitter.isDisposed) {
            OBDLogger.log(TAG, "[${item.request.tag}] disposed while waiting in queue")
            return
        }
        try {
            OBDLogger.log(TAG, "Executing [${item.request.tag}] at [${Date()}]")
            val response = item.request
                    .execute(obdDevice)
                    .subscribe(
                            {
                                when (item.responseEmitter.isDisposed) {
                                    true -> OBDLogger.log(TAG, "[${item.request.tag}] disposed while emitting value")
                                    false -> item.responseEmitter.onNext(it)
                                }
                            },
                            {
                                when (item.responseEmitter.isDisposed) {
                                    true -> OBDLogger.log(TAG, "[${item.request.tag}] disposed while emitting error")
                                    false -> item.responseEmitter.onError(it)
                                }
                            },
                            {
                                when (item.responseEmitter.isDisposed) {
                                    true -> OBDLogger.log(TAG, "[${item.request.tag}] disposed while emitting completion indicator")
                                    false -> {
                                        item.responseEmitter.onComplete()
                                        OBDLogger.log(TAG, "Request Executed [${item.request.tag}]")
                                    }
                                }
                            })

            item.responseEmitter.setCancellable {
                OBDLogger.log(TAG, "[${item.request.tag}] disposed while executing")
                response.dispose()
            }


        } catch (e: ExecutionException) {
            OBDLogger.log(TAG, "Exception while executing [${item.request.tag}]", e)
        }
    }


}


interface IOBDRequestPipe {
    fun <T : OBDResponse> submitRequest(request: OBDRequest): Observable<T>
}
