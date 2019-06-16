package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.crashlytics.android.Crashlytics
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import redux.StoreChangeDisposable
import redux.api.Store
import timber.log.Timber
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

fun BluetoothDevice.connect(): OBDConnection {
    /* http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
    * #createRfcommSocketToServiceRecord(java.util.UUID)
    *
    * "Hint: If you are connecting to a Bluetooth serial board then try using the
    * well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you
    * are connecting to an Android peer then please generate your own unique
    * UUID."
    */
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var sock: BluetoothSocket
    var sockFallback: BluetoothSocket?

    Timber.d("BluetoothDeviceExt: Starting Bluetooth connection..")
    try {
        sock = this.createRfcommSocketToServiceRecord(MY_UUID)
        sock.connect()
    } catch (e1: Exception) {
        Timber.d(e1, "BluetoothDeviceExt: There was an error while establishing Bluetooth connection. Falling back..")
        val clazz = this::class.java
        val paramTypes = arrayOf<Class<*>>(Integer.TYPE)
        try {
            val m = clazz.getMethod("createRfcommSocket", *paramTypes)
            val params = arrayOf<Any>(Integer.valueOf(1))
            sockFallback = m.invoke(this, *params) as BluetoothSocket
            sockFallback.connect()
            sock = sockFallback
        } catch (e2: Exception) {
            Crashlytics.getInstance().core.logException(Exception("Unable to establish Bluetooth Connection", e2))
            Timber.e(e2, "BluetoothDeviceExt: Couldn't fallback while establishing Bluetooth connection.")
            throw IOException(e2.message)
        }

    }
    return BluetoothOBDConnection(sock)
}


fun <S : Any> Store<S>.asCustomObservable(): Observable<S> {
    return Observable.create(StoreChangeOnSubscribe(this))
}

class StoreChangeOnSubscribe<S : Any>(private val store: Store<S>) : ObservableOnSubscribe<S> {

    override fun subscribe(emitter: ObservableEmitter<S>) {
        val subscription = store.subscribe {
            if (!emitter.isDisposed) {
                emitter.onNext(store.state)
            }
        }

        if (!emitter.isDisposed) {
            emitter.onNext(store.state)
        }

        emitter.setDisposable(object : StoreChangeDisposable() {
            override fun onDispose() {
                subscription.unsubscribe()
            }
        })
    }

}

interface OBDConnection : Closeable {
    val inputStream: InputStream
    val outputStream: OutputStream
}

private class BluetoothOBDConnection(private val socket: BluetoothSocket) : OBDConnection {
    override val inputStream: InputStream
        get() = socket.inputStream
    override val outputStream: OutputStream
        get() = socket.outputStream

    override fun close() {
        socket.close()
    }

}