package com.exp.carconnect.dashboard.activity

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.exp.carconnect.base.activity.SetupActivity
import com.exp.carconnect.dashboard.R
import com.exp.carconnect.dashboard.view.Dashboard
import com.exp.carconnect.dashboard.viewmodel.OBDDashboard
import com.exp.carconnect.dashboard.viewmodel.OBDDashboardVM


class DashboardActivity : AppCompatActivity() {

    companion object {
        const val TAG = "DashboardActivity"
        const val SETUP_REQUEST_CODE = 1001
    }

    private lateinit var dashboard: Dashboard
    private lateinit var dashboardVM: OBDDashboardVM


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setupView()
        startActivityForResult(Intent(this, SetupActivity::class.java), SETUP_REQUEST_CODE)


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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETUP_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            dashboardVM = ViewModelProviders.of(this).get(OBDDashboardVM::class.java)
            dashboardVM.dashboardLiveData.observe(this, Observer<OBDDashboard> {
                setupDashboard(it!!)
            })
        } else {
            finish()
        }
    }
}