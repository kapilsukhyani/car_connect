package com.exp.carconnect.base


class OBDRequestPipeTest {
//    private val TAG = "OBDRequestPipeTest"
//
//
//    @Test
//    fun testSubmit() {
//        val pipe = OBDRequestPipe(Schedulers.io(), Schedulers.computation(), object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.just("0102")
//            }
//        })
//
//        val io = Schedulers.io()
//        io.createWorker().schedule {
//            Logger.log(TAG, "posting request 1")
//            pipe.submitRequest<RPMResponse>(RPMRequest()).subscribe({ response ->
//                System.out.println()
//                Logger.log(TAG, "response1 received" + response.tag)
//
//            })
//        }
//        io.createWorker().schedule {
//            Logger.log(TAG, "posting request 2")
//            pipe.submitRequest<RPMResponse>(RPMRequest()).subscribe({ response ->
//                Logger.log(TAG, "response2 received" + response.tag)
//
//
//            }).dispose() // disposing this request should not get executed
//        }
//        io.createWorker().schedule {
//            Logger.log(TAG, "posting request 3")
//            pipe.submitRequest<RPMResponse>(RPMRequest()).subscribe({ response ->
//                Logger.log(TAG, "response3 received" + response.tag)
//
//
//            })
//        }
//        io.createWorker().schedule {
//            Logger.log(TAG, "posting request 4")
//            pipe.submitRequest<RPMResponse>(RPMRequest()).subscribe({ response ->
//                Logger.log(TAG, "response4 received" + response.tag)
//
//
//            })
//        }
//        Logger.log(TAG, "waiting for responses")
//        Thread.sleep(15000)
//    }
//
//    @Test
//    fun testMultiRequest() {
//
//        val pipe = OBDRequestPipe(Schedulers.io(), Schedulers.computation(), object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.just("0102")
//            }
//        })
//
//        val io = Schedulers.io()
//        io.createWorker().schedule {
//            Logger.log(TAG, "posting multi request")
//            pipe.submitRequest<RPMResponse>(OBDMultiRequest("Init", listOf(RPMRequest(), RPMRequest()))).subscribe({ response ->
//                System.out.println()
//                Logger.log(TAG, "multi request response received " + response.tag)
//
//            })
//        }
//        Logger.log(TAG, "waiting for responses")
//        Thread.sleep(10000)
//
//    }


}