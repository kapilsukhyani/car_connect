package com.exp.carconnect.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.support.annotation.StringRes
import com.crashlytics.android.Crashlytics
import com.exp.carconnect.obdlib.SimulatedStreams
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


fun AndroidViewModel.getString(@StringRes id: Int,
                               vararg formatArgs: Any = emptyArray()): String = if (formatArgs.isEmpty()) {
    getApplication<Application>().getString(id)
} else {
    getApplication<Application>().getString(id, formatArgs)
}

private fun BluetoothDevice.connect(): OBDConnection {
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

sealed class OBDDongle(val name: String,
                       val address: String) {
    abstract fun connect(): OBDConnection
    abstract fun connectable(): Boolean
}


interface OBDDongleLoader {
    fun loadDevices(includeSimulator: Boolean = true): Set<OBDDongle> = loadDevices().let { loadedDevices ->
        val set = mutableSetOf<OBDDongle>()
        if (includeSimulator) {
            set.add(SimulatedOBDDongle())
        }
        set.addAll(loadedDevices)
        set
    }

    fun loadDevices(): Set<OBDDongle>
}


fun OBDDongle(device: BluetoothDevice): OBDDongle = BluetoothOBDDongle(device)
fun OBDDongleLoader(): OBDDongleLoader = BluetoothOBDDongleLoader()

private class BluetoothOBDConnection(private val socket: BluetoothSocket) : OBDConnection {
    override val inputStream: InputStream
        get() = socket.inputStream
    override val outputStream: OutputStream
        get() = socket.outputStream

    override fun close() {
        socket.close()
    }
}

class BluetoothOBDDongle(private val device: BluetoothDevice) : OBDDongle(device.name, device.address) {
    override fun connect(): OBDConnection = device.connect()
    override fun connectable(): Boolean = BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false
}

class SimulatedOBDDongle : OBDDongle("Simulator", "Dummy") {
    private val simulatedStreams = SimulatedStreams()
    override fun connect(): OBDConnection = object : OBDConnection {
        override val inputStream: InputStream
            get() = simulatedStreams.inputStream
        override val outputStream: OutputStream
            get() = simulatedStreams.outputStream

        override fun close() {
            //no op
        }

    }

    override fun connectable(): Boolean = true

}

private class BluetoothOBDDongleLoader : OBDDongleLoader {
    override fun loadDevices(): Set<OBDDongle> = BluetoothAdapter.getDefaultAdapter()
            ?.bondedDevices
            ?.map { bondedBTDevice -> OBDDongle(bondedBTDevice) }
            ?.toSet()
            ?: emptySet()
}