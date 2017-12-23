package com.exp.carconnect.basic.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.exp.carconnect.Logger
import com.exp.carconnect.OBDMultiRequest
import com.exp.carconnect.basic.CarConnectApp
import com.exp.carconnect.basic.OBDEngine
import com.exp.carconnect.basic.di.Main
import com.exp.carconnect.basic.obdmessage.*
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class OBDDashboardVM(app: Application) : AndroidViewModel(app) {
    companion object {
        val TAG = "DashboardVM"
        private val FUEL_FACTOR = 100.0f
        private val RPM_FACTOR = 1000.0f
        private val vinRequest = VinRequest()
        private val fuelLevelRequest = FuelLevelRequest(repeatable = IsRepeatable.Yes(1, TimeUnit.MINUTES))
        private val restDataRequest = OBDMultiRequest("Dashboard",
                listOf(SpeedRequest(),
                        RPMRequest(),
                        PendingTroubleCodesRequest(TroubleCodeCommandType.ALL),
                        IgnitionMonitorRequest(),
                        DTCNumberRequest()),
                IsRepeatable.Yes(50, TimeUnit.MILLISECONDS))
    }

    private val mutableDashboardLiveData: MutableLiveData<OBDDashboard> = MutableLiveData()
    val dashboardLiveData: LiveData<OBDDashboard> = mutableDashboardLiveData


    private val subscriptions = CompositeDisposable()

    @Inject
    lateinit var obdEngine: OBDEngine
    @Inject
    @field:Main
    lateinit var mainScheduler: Scheduler


    init {
        (app as CarConnectApp).newConnectionComponent!!.inject(this)
        mutableDashboardLiveData.value = OBDDashboard()
        startListeningToData()
    }

    private fun startListeningToData() {
        subscriptions.add(obdEngine.submit<VinResponse>(vinRequest)
                .observeOn(mainScheduler)
                .subscribe({ it -> mutableDashboardLiveData.value = mutableDashboardLiveData.value?.copy(vin = it.vin) },
                        { it -> Logger.log(TAG, "exception while loading vin", it) },
                        { Logger.log(TAG, "Vin loader completed") }))

        subscriptions.add(obdEngine.submit<FuelLevelResponse>(fuelLevelRequest)
                .observeOn(mainScheduler)
                .subscribe({ it -> mutableDashboardLiveData.value = mutableDashboardLiveData.value?.copy(fuel = it.fuelLevel / FUEL_FACTOR) },
                        { it -> Logger.log(TAG, "exception while loading fuel", it) },
                        { Logger.log(TAG, "Fuel loader completed") }))

        subscriptions.add(obdEngine.submit<OBDResponse>(restDataRequest)
                .observeOn(mainScheduler)
                .subscribe({ response ->
                    when (response) {
                        is SpeedResponse -> {
                            mutableDashboardLiveData.value = mutableDashboardLiveData.value?.copy(speed = response.metricSpeed.toFloat())
                        }

                        is RPMResponse -> {
                            mutableDashboardLiveData.value = mutableDashboardLiveData.value?.copy(rpm = response.rpm / RPM_FACTOR)
                        }

                        is IgnitionMonitorResponse -> {
                            mutableDashboardLiveData.value = mutableDashboardLiveData.value?.copy(ignition = response.ignitionOn)
                        }

                        is DTCNumberResponse -> {
                            mutableDashboardLiveData.value = mutableDashboardLiveData.value?.copy(checkEngineLight = response.milOn)
                        }

                    }
                },
                        { it -> Logger.log(TAG, "exception while loading multi params", it) },
                        { Logger.log(TAG, "Multi params loader completed") }))
    }


    override fun onCleared() {
        super.onCleared()
        subscriptions.dispose()
    }
}

data class OBDDashboard(val online: Boolean = false, val vin: String = "", val rpm: Float = 0.0f,
                        val speed: Float = 0.0f, val fuel: Float = 0.0f,
                        val ignition: Boolean = false, val checkEngineLight: Boolean = false)