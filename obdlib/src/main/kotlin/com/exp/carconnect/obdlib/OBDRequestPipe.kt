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
                    // this is just a small trick to return strongly typed values instead os letting callers typecast each time
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
        obdRequestPipe
                .serialize()
                .toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(ioScheduler)
                .subscribe(
                        { item ->
                            Logger.log(TAG, "Submitted [${item.request.tag}] at [${Date()}]")
                            executeRequestIfObserverAlive(item)
                        },
                        { error ->
                            Logger.log(TAG, error)
                        },
                        {
                            Logger.log(TAG, "request pipe completed")
                        })

    }


    private fun executeRequestIfObserverAlive(item: Item) {
        if (item.responseEmitter.isDisposed) {
            Logger.log(TAG, "[${item.request.tag}] disposed while waiting in queue")
            return
        }
        try {
            Logger.log(TAG, "Executing [${item.request.tag}] at [${Date()}]")
            val response = item.request
                    .execute(obdDevice)
                    .subscribe(
                            {
                                when (item.responseEmitter.isDisposed) {
                                    true -> Logger.log(TAG, "[${item.request.tag}] disposed while emitting value")
                                    false -> item.responseEmitter.onNext(it)
                                }
                            },
                            {
                                when (item.responseEmitter.isDisposed) {
                                    true -> Logger.log(TAG, "[${item.request.tag}] disposed while emitting error")
                                    false -> item.responseEmitter.onError(it)
                                }
                            },
                            {
                                when (item.responseEmitter.isDisposed) {
                                    true -> Logger.log(TAG, "[${item.request.tag}] disposed while emitting completion indicator")
                                    false -> {
                                        item.responseEmitter.onComplete()
                                        Logger.log(TAG, "Request Executed [${item.request.tag}]")
                                    }
                                }
                            })

            item.responseEmitter.setCancellable {
                Logger.log(TAG, "[${item.request.tag}] disposed while executing")
                response.dispose()
            }


        } catch (e: ExecutionException) {
            Logger.log(TAG, "Exception while executing [${item.request.tag}]", e)
        }
    }


}


interface IOBDRequestPipe {
    fun <T : OBDResponse> submitRequest(request: OBDRequest): Observable<T>
}
