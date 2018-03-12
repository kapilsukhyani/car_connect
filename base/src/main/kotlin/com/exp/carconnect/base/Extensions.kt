package com.exp.carconnect.base

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

fun BluetoothDevice.connect(): BluetoothSocket {
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

    Log.d(CarConnectAbstractApp.TAG, "Starting Bluetooth connection..")
    try {
        sock = this.createRfcommSocketToServiceRecord(MY_UUID)
        sock.connect()
    } catch (e1: Exception) {
        Log.e(CarConnectAbstractApp.TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1)
        val clazz = this::class.java
        val paramTypes = arrayOf<Class<*>>(Integer.TYPE)
        try {
            val m = clazz.getMethod("createRfcommSocket", *paramTypes)
            val params = arrayOf<Any>(Integer.valueOf(1))
            sockFallback = m.invoke(this, *params) as BluetoothSocket
            sockFallback.connect()
            sock = sockFallback
        } catch (e2: Exception) {
            Log.e(CarConnectAbstractApp.TAG, "Couldn't fallback while establishing Bluetooth connection.", e2)
            throw IOException(e2.message)
        }

    }

    return sock
}
