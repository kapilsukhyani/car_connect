package com.exp.carconnect.basic


class OBDMultiRequestTest {
//
//    @Test
//    fun testExecuteSucceeded() {
//        val testRPMResponse = RPMResponse()
//        val testResponse = object : OBDResponse("SomeResponse") {
//            override fun getFormattedResult(): String {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//        }
//        val request1 = object : OBDRequest("RPMRequest", "rpm") {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                return testRPMResponse
//            }
//        }
//        val request2 = object : OBDRequest("SomeOtherRequest", "req") {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                return testResponse
//            }
//        }
//        val request = OBDMultiRequest("Dashboard", listOf(request1, request2))
//
//        val count = AtomicInteger(0)
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    count.incrementAndGet()
//                    Observable.just("0102")
//                }
//
//            }
//        }
//        val subscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(subscriber)
//        Assert.assertEquals(0, subscriber.errorCount())
//        Assert.assertEquals(2, subscriber.valueCount())
//        Assert.assertTrue(subscriber.values().contains(testRPMResponse))
//        Assert.assertTrue(subscriber.values().contains(testResponse))
//    }
//
//    @Test
//    fun testExecuteContinuedWhenSomeRequestFailed() {
//        val testRPMResponse = RPMResponse()
//        val testResponse = object : OBDResponse("SomeResponse") {
//            override fun getFormattedResult(): String {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//        }
//        var failedResponse: OBDResponse? = null
//        val request1 = object : OBDRequest("RPMRequest", "rpm") {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                return testRPMResponse
//            }
//        }
//        val request2 = object : OBDRequest("SomeOtherRequest", "req") {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                return testResponse
//            }
//        }
//        val request = object : OBDMultiRequest("Dashboard", listOf(request1, request2)) {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//            override fun getFailedOBDResponse(exception: ExecutionException): OBDResponse {
//                failedResponse = FailedOBDResponse(exception)
//                return failedResponse as FailedOBDResponse
//            }
//        }
//
//
//        val retryCount = AtomicInteger(0)
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    if (command.equals(request1.command)) {
//                        retryCount.incrementAndGet()
//                        Observable.error<String>(NoDataException(command, "NO DATA"))
//                    } else {
//                        Observable.just("0102")
//                    }
//                }
//
//            }
//        }
//        val subscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(subscriber)
//        Assert.assertEquals(0, subscriber.errorCount())
//        Assert.assertEquals(2, subscriber.valueCount())
//        Assert.assertEquals(3, retryCount.get())
//        Assert.assertTrue(subscriber.values().contains(failedResponse))
//        Assert.assertTrue(subscriber.values().contains(testResponse))
//    }
//
//    @Test
//    fun testExecuteAllFailed() {
//
//        val failedResponses = listOf<FailedOBDResponse>()
//        val request1 = RPMRequest()
//        val request2 = RPMRequest()
//        val request3 = RPMRequest()
//
//        val request = object : OBDMultiRequest("Dashboard", listOf(request1, request2, request3)) {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                return null as OBDResponse
//            }
//
//            override fun getFailedOBDResponse(exception: ExecutionException): OBDResponse {
//                val failedResponse = FailedOBDResponse(exception)
//                failedResponses.plus(failedResponse)
//                return failedResponse
//            }
//        }
//
//
//        val retryCount = AtomicInteger(0)
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    retryCount.incrementAndGet()
//                    Observable.error<String>(NoDataException(command, "NO DATA"))
//                }
//
//            }
//        }
//        val subscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(subscriber)
//        Assert.assertEquals(0, subscriber.errorCount())
//        Assert.assertEquals(3, subscriber.valueCount())
//        Assert.assertEquals(9, retryCount.get())
//        Assert.assertTrue(subscriber.values().containsAll(failedResponses))
//    }
//
//
//    @Test
//    fun testExecuteDisContinuedWhenDisposed() {
//        val testRPMResponse = RPMResponse()
//        val request1 = object : OBDRequest("RPMRequest", "rpm") {
//            override fun toResponse(buffer: List<Int>, rawResponse: String): OBDResponse {
//                return testRPMResponse
//            }
//        }
//        val request2 = RPMRequest()
//        val request3 = RPMRequest()
//
//        val request = OBDMultiRequest("Dashboard", listOf(request1, request2, request3))
//
//
//        val count = AtomicInteger(0)
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    count.incrementAndGet()
//                    Observable.just<String>("0102")
//                }
//
//            }
//        }
//        val subscriber = object : TestObserver<OBDResponse>() {
//            override fun onNext(t: OBDResponse) {
//                super.onNext(t)
//                if (count.get() == 1) {
//                    dispose()
//                }
//
//            }
//        }
//        request.execute(device).subscribe(subscriber)
//        Assert.assertEquals(0, subscriber.errorCount())
//        Assert.assertEquals(1, subscriber.valueCount())
//        Assert.assertEquals(1, count.get())
//        Assert.assertTrue(subscriber.values().contains(testRPMResponse))
//    }

}