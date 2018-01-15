package com.exp.carconnect.basic.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.exp.carconnect.basic.R
import com.exp.carconnect.basic.view.Dashboard
import com.exp.carconnect.basic.viewmodel.OBDDashboard
import com.exp.carconnect.basic.viewmodel.OBDDashboardVM


class DashboardActivity : AppCompatActivity() {

    companion object {
        val TAG = "MainActivity"
    }

    private lateinit var dashboard: Dashboard
    private lateinit var dashboardVM: OBDDashboardVM


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setupView()
        dashboardVM = ViewModelProviders.of(this).get(OBDDashboardVM::class.java)
        dashboardVM.dashboardLiveData.observe(this, Observer<OBDDashboard> {
            setupDashboard(it!!)
        })
    }

    private fun setupView() {
        dashboard = findViewById(R.id.dashboard)
        dashboard.onVINChangedListener = fun(s: String) {
            println("Vin: " + s)

        }
        dashboard.onOnlineChangedListener = fun(aBoolean: Boolean?) {
            println("Online: " + aBoolean!!)

        }
        dashboard.setOnIgnitionChangedListener({ aBoolean ->
            println("Ignition: " + aBoolean)

        })

        dashboard.setOnCheckEngineLightChangedListener({ aBoolean ->
            println("Check Engine Light: " + aBoolean)

        })
        dashboard.setOnFuelPercentageChangedListener({ aFloat ->
            println("Fuel: " + aFloat)

        })
        dashboard.setOnSpeedChangedListener({ aFloat ->
            println("SPEED: " + aFloat)

        })

        dashboard.setOnRPMChangedListener({ aFloat ->
            println("RPM: " + aFloat)
        })
        dashboard.online = true
        dashboard.rpmDribbleEnabled = true
        dashboard.speedDribbleEnabled = true
    }


    private fun setupDashboard(dashboardData: OBDDashboard) {
        dashboard.currentSpeed = dashboardData.speed
        dashboard.currentRPM = dashboardData.rpm
        dashboard.vin = dashboardData.vin
        dashboard.showIgnitionIcon = dashboardData.ignition
        dashboard.showCheckEngineLight = dashboardData.checkEngineLight
        dashboard.fuelPercentage = dashboardData.fuel
        dashboard.currentAirIntakeTemp = dashboardData.currentAirIntakeTemp
        dashboard.currentAmbientTemp = dashboardData.currentAmbientTemp
    }
}