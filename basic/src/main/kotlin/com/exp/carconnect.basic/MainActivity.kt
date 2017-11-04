package com.exp.carconnect.basic

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.exp.carconnect.OBDMultiRequest
import com.exp.carconnect.basic.obdmessage.*
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : Activity() {

    private lateinit var engine: OBDEngine
    private lateinit var speed: TextView
    private lateinit var rpm: TextView
    private lateinit var vin: TextView
    private lateinit var troubleCodes: TextView
    private lateinit var ignition: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        speed = findViewById(R.id.speed)
        rpm = findViewById(R.id.rpm)
        vin = findViewById(R.id.vin)
        troubleCodes = findViewById(R.id.ptc)
        ignition = findViewById(R.id.ignition)

        initEngine()
    }

    companion object {
        val TAG = "MainActivity"
        /*
     * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
     * #createRfcommSocketToServiceRecord(java.util.UUID)
     *
     * "Hint: If you are connecting to a Bluetooth serial board then try using the
     * well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you
     * are connecting to an Android peer then please generate your own unique
     * UUID."
     */
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    fun BluetoothDevice.connect(): BluetoothSocket {
        var sock: BluetoothSocket
        var sockFallback: BluetoothSocket?

        Log.d(TAG, "Starting Bluetooth connection..")
        try {
            sock = this.createRfcommSocketToServiceRecord(MY_UUID)
            sock.connect()
        } catch (e1: Exception) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1)
            val clazz = this::class.java
            val paramTypes = arrayOf<Class<*>>(Integer.TYPE)
            try {
                val m = clazz.getMethod("createRfcommSocket", *paramTypes)
                val params = arrayOf<Any>(Integer.valueOf(1))
                sockFallback = m.invoke(this, *params) as BluetoothSocket
                sockFallback.connect()
                sock = sockFallback
            } catch (e2: Exception) {
                Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2)
                throw IOException(e2.message)
            }

        }

        return sock
    }


    private fun initEngine() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.bondedDevices?.let {
            if (adapter.isEnabled) {
                val socket = it.filter { it.name?.contains("OBD")!! }[0].connect()
                engine = OBDEngine(socket.inputStream, socket.outputStream)
                prepareOBD()
            }

        } ?: Toast.makeText(this, "No bluetooth", Toast.LENGTH_LONG).show()
    }

    private fun prepareOBD() {
        engine.submit<OBDResponse>(OBDResetCommand())
                .doOnNext {
                    Log.d(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
                }
                .flatMap {
                    engine
                            .submit<OBDResponse>(OBDMultiRequest("Initialization",
                                    listOf(EchoOffCommand(), EchoOffCommand(), LineFeedOffCommand(), TimeoutCommand(62))))
                            .delaySubscription(500, TimeUnit.MILLISECONDS)
                }
                .doOnComplete {
                    setupDashboard()
                }
                .subscribe {
                    Log.d(TAG, "Got response ${it::class.java.simpleName}[${it.getFormattedResult()}]")
                }
    }

    private fun setupDashboard() {
        engine.submit<OBDResponse>(OBDMultiRequest("Dashboard",
                listOf(SpeedRequest(),
                        RPMRequest(),
                        VinRequest(),
                        PendingTroubleCodesRequest(TroubleCodeCommandType.ALL),
                        IgnitionMonitorRequest()),
                IsRepeatable.Yes(1, TimeUnit.SECONDS)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    Log.d(TAG, "Got response ${response::class.java.simpleName}[${response.getFormattedResult()}]")
                    when (response) {
                        is SpeedResponse -> {
                            speed.text = response.getFormattedResult()
                        }

                        is RPMResponse -> {
                            rpm.text = response.getFormattedResult()
                        }

                        is VinResponse -> {
                            vin.text = response.vin
                        }

                        is PendingTroubleCodesResponse -> {
                            troubleCodes.text = response.codes.toString()
                        }
                        is IgnitionMonitorResponse -> {
                            ignition.text = response.ignitionOn.toString()
                        }

                    }
                }, { exception ->
                    Log.d(TAG, "Got error ${exception.cause}")
                    exception.cause?.printStackTrace()
                }, {
                    Log.d(TAG, "completed")
                })
    }
}