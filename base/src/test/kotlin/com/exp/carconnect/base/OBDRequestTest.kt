package com.exp.carconnect.base

import com.exp.carconnect.base.obdmessage.OBDRequest
import com.exp.carconnect.base.obdmessage.OBDResponse


class OBDRequestTest {


//    private val testResponse = object : OBDResponse("Response") {}
//    private val request = object : OBDRequest(tag = "TestRequest", command = "TestCommand") {
//        override fun toResponse(buffer: List<Int>): OBDResponse {
//            return testResponse
//        }
//    }
//
//
//    @Test
//    fun testExecuteRetriedThreeTimesWhenFailed() {
//        val actualException = NoDataException("TestRequest", "No Data")
//        val count = AtomicInteger(0)
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    count.incrementAndGet()
//                    Observable.error<String>(actualException)
//                }
//
//            }
//        }
//
//        val testSubscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(testSubscriber)
//        Assert.assertEquals(3, count.get())
//        Assert.assertEquals(actualException, (testSubscriber.errors()[0] as ExecutionException).cause)
//    }
//
//    @Test
//    fun testExecuteRetriedAndThenSucceeded() {
//        val actualException = NoDataException("TestRequest", "No Data")
//        val count = AtomicInteger(0)
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    if (count.get() == 2) {
//                        Observable.just("0102")
//                    } else {
//                        count.incrementAndGet()
//                        Observable.error<String>(actualException)
//                    }
//                }
//
//            }
//        }
//
//        val testSubscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(testSubscriber)
//        Assert.assertEquals(2, count.get())
//        Assert.assertEquals(0, testSubscriber.errorCount())
//        Assert.assertEquals(1, testSubscriber.valueCount())
//        Assert.assertEquals(testResponse, testSubscriber.values()[0])
//
//    }
//
//    @Test
//    fun testExecuteSucceeded() {
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
//
//        val testSubscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(testSubscriber)
//        Assert.assertEquals(1, count.get())
//        Assert.assertEquals(0, testSubscriber.errorCount())
//        Assert.assertEquals(1, testSubscriber.valueCount())
//        Assert.assertEquals(testResponse, testSubscriber.values()[0])
//
//    }
//
//
//    @Test
//    fun testExecuteDisposedProperly() {
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
//
//        val testSubscriber = object: TestObserver<OBDResponse>(){
//            override fun onSubscribe(s: Disposable) {
//                super.onSubscribe(s)
//                s.dispose()
//            }
//        }
//        request.execute(device).subscribe(testSubscriber)
//        Assert.assertEquals(0, count.get())
//        Assert.assertEquals(0, testSubscriber.errorCount())
//        Assert.assertEquals(0, testSubscriber.valueCount())
//    }
//
//    @Test
//    fun testExecuteRequestNotRetriedWhenSet() {
//        val count = AtomicInteger(0)
//        val actualException = NoDataException("TestRequest", "No Data")
//
//        val device = object : IOBDDevice {
//            override fun run(command: String): Observable<String> {
//                return Observable.defer {
//                    count.incrementAndGet()
//                    Observable.error<String>(actualException)
//                }
//
//            }
//        }
//        val request = object : OBDRequest(tag = "TestRequest", command = "TestCommand", retriable = false) {
//            override fun toResponse(buffer: List<Int>): OBDResponse {
//                return testResponse
//            }
//        }
//
//        val testSubscriber = TestObserver<OBDResponse>()
//        request.execute(device).subscribe(testSubscriber)
//        Assert.assertEquals(1, count.get())
//        Assert.assertEquals(1, testSubscriber.errorCount())
//        Assert.assertEquals(actualException, (testSubscriber.errors()[0] as ExecutionException).cause)
//
//    }

}