package com.exp.carconnect.base

import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSubject() {
        val latch = CountDownLatch(1)

        val sub = PublishSubject.create<Int>()
        val ob = object : Observer<Int> {
            override fun onNext(t: Int) {

                System.out.println("onNext " + t + " " + Thread.currentThread().name)
                Thread.sleep(1000)
                System.out.println("onNext  executed " + t)

            }

            override fun onComplete() {
                System.out.println("onCompleted")
                latch.countDown()

            }

            override fun onError(e: Throwable) {
            }

            override fun onSubscribe(d: Disposable) {
            }
        }

        sub.serialize().observeOn(Schedulers.computation()).subscribe(ob)


        val t1 = Thread({
            sub.onNext(1)
            System.out.println("1. Done Emitting")
        }, "Thread1")
        val t2 = Thread({
            sub.onNext(2)
            System.out.println("2. Done Emitting")

        }, "Thread2")
        val t3 = Thread({
            sub.onNext(3)
            System.out.println("3. Done Emitting")

        }, "Thread3")


        t1.start()
        t2.start()
        t3.start()

        t1.join()
        t2.join()
        t3.join()

        sub.onComplete()
        latch.await()


    }


    @Test
    fun test1() = runBlocking {
        var job = launch(CommonPool) {
            try {
                repeat(5, { i ->
                    System.out.println("helo " + i)
                    delay(500L)
                })
            } finally {
                System.out.println("canceled")
            }
        }
        val result = job.cancel(RuntimeException("down"))
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    val lock = Object()
    val stringChannel = Channel<String>()
    val singleThreadedContext = newSingleThreadContext("Producer Context")

    fun sendString(data: String) = launch(singleThreadedContext) {
        //        synchronized(lock) {
        log("Producing data {$data}")
        stringChannel.send(data)
        log("Data produced {$data}")
//        }
    }

    fun processString() = launch(CommonPool + CoroutineName("Consumer")) {
        stringChannel.consumeEach {
            log("Consuming data {$it}")
            delay(1000)
            log("Data consumed {$it}")

        }
    }

    @Test
    fun testChannel() = runBlocking {
        log("Starting test")

        val dataProcessor = processString()

        val job1 = sendString("hello1")
        val job2 = sendString("hello2")
        val job3 = sendString("hello3")
        val job4 = sendString("hello4")

        delay(5000)

        val closed = stringChannel.close()
        log("Completing test {$closed} ")
    }

    val requestPipe = PublishSubject.create<Single<Any>>()

    fun submitRequest(request: Object): Single<Any> {
        val requstob = Single.create<Any> { emmitter ->
            emmitter.onError(RuntimeException())
        }

        requestPipe.onNext(requstob)
        return requstob
    }


    @Test
    fun testSingle() {
        val singleString = Single.create<String> { emitter ->
            try {

                Thread.sleep(2000)
            } catch (e: Exception) {
                println("I am interrupted")
            }
            if (emitter.isDisposed) {
                println("I am disposed")
            }
            emitter.onSuccess("Hey I am completed")
        }

        val subscription = singleString
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { item ->
                    println("Got an item {$item}")
                }
//        Thread.sleep(500)
//        subscription.dispose()

        Thread.sleep(3000)


    }


    @Test
    fun testSubjectAgain() {

    }

}
