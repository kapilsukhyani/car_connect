package com.exp.carconnect.dashboard.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import com.exp.carconnect.Logger
import com.exp.carconnect.base.di.Main
import com.exp.carconnect.dashboard.DashboardAppContract
import com.exp.carconnect.obdlib.OBDMultiRequest
import com.exp.carconnect.obdlib.obdmessage.*
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
        private val temperatureRequest = OBDMultiRequest("Temperature", listOf(TemperatureRequest(TemperatureType.AIR_INTAKE),
                TemperatureRequest(TemperatureType.AMBIENT_AIR)),
                IsRepeatable.Yes(500, TimeUnit.MILLISECONDS))
        private val restDataRequest = OBDMultiRequest("Dashboard",
                listOf(SpeedRequest(),
                        RPMRequest(),
                        PendingTroubleCodesRequest(TroubleCodeCommandType.ALL),
                        IgnitionMonitorRequest(),
                        DTCNumberRequest()),
                IsRepeatable.Yes(50, TimeUnit.MILLISECONDS))
    }

    private val obdResponseLiveData: MutableLiveData<OBDDashboard> = MutableLiveData()
    val dashboardLiveData: MediatorLiveData<OBDDashboard> = MediatorLiveData()


    private val subscriptions = CompositeDisposable()


    @Inject
    lateinit var obdEngine: com.exp.carconnect.obdlib.OBDEngine
    @Inject
    @field:Main
    lateinit var mainScheduler: Scheduler


    init {
        val initialSnapshot = OBDDashboard()
        (app as DashboardAppContract).buildNewDashboardComponent()
        (app as DashboardAppContract).dashboardComponent!!.inject(this)

        dashboardLiveData.addSource(obdResponseLiveData, {
            dashboardLiveData.value = it
        })
        obdResponseLiveData.value = initialSnapshot
        dashboardLiveData.value = initialSnapshot
        startListeningToData()
    }

    private fun startListeningToData() {
        subscriptions.add(obdEngine.submit<VinResponse>(vinRequest)
                .observeOn(mainScheduler)
                .subscribe({ it -> obdResponseLiveData.value = obdResponseLiveData.value?.copy(vin = it.vin) },
                        { it -> Logger.log(TAG, "exception while loading vin", it) },
                        { Logger.log(TAG, "Vin loader completed") }))

        subscriptions.add(obdEngine.submit<OBDResponse>(temperatureRequest)
                .observeOn(mainScheduler)
                .subscribe({ response ->
                    when ((response as TemperatureResponse).type) {
                        TemperatureType.AIR_INTAKE -> {
                            obdResponseLiveData.value = obdResponseLiveData.value?.copy(currentAirIntakeTemp =
                            response.temperature.toFloat())
                        }
                        TemperatureType.AMBIENT_AIR -> {
                            obdResponseLiveData.value = obdResponseLiveData.value?.copy(currentAmbientTemp =
                            response.temperature.toFloat())
                        }
                    }
                },
                        { it -> Logger.log(TAG, "exception while loading temperature", it) },
                        { Logger.log(TAG, "Temperature loader completed") }))

        subscriptions.add(obdEngine.submit<FuelLevelResponse>(fuelLevelRequest)
                .observeOn(mainScheduler)
                .subscribe({ it -> obdResponseLiveData.value = obdResponseLiveData.value?.copy(fuel = it.fuelLevel / FUEL_FACTOR) },
                        { it -> Logger.log(TAG, "exception while loading fuel", it) },
                        { Logger.log(TAG, "Fuel loader completed") }))

        subscriptions.add(obdEngine.submit<OBDResponse>(restDataRequest)
                .observeOn(mainScheduler)
                .subscribe({ response ->
                    when (response) {
                        is SpeedResponse -> {
                            obdResponseLiveData.value = obdResponseLiveData.value?.copy(speed = response.metricSpeed.toFloat())
                        }

                        is RPMResponse -> {
                            obdResponseLiveData.value = obdResponseLiveData.value?.copy(rpm = response.rpm / RPM_FACTOR)
                        }

                        is IgnitionMonitorResponse -> {
                            obdResponseLiveData.value = obdResponseLiveData.value?.copy(ignition = response.ignitionOn)
                        }

                        is DTCNumberResponse -> {
                            obdResponseLiveData.value = obdResponseLiveData.value?.copy(checkEngineLight = response.milOn)
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
                        val ignition: Boolean = false, val checkEngineLight: Boolean = false,
                        val currentAirIntakeTemp: Float = 0.0f, val currentAmbientTemp: Float = 0.0f)