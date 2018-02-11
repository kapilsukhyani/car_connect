package com.exp.carconnect.base.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.exp.carconnect.base.R
import com.exp.carconnect.base.viewmodel.SetupScreenVM
import com.exp.carconnect.base.viewmodel.SetupStatus


class SetupActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var dashboardButton: Button
    private lateinit var setupScreenVM: SetupScreenVM

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        setupScreenVM = ViewModelProviders.of(this).get(SetupScreenVM::class.java)
        statusView = findViewById(R.id.status)
        dashboardButton = findViewById(R.id.showDashboardButton)

        setupScreenVM.setupStatusLiveData.observe(this, Observer<SetupStatus> {

            when (it) {
                is SetupStatus.Completed -> {
                    dashboardButton.visibility = View.VISIBLE
                    statusView.text = "Setup Completed, Press 'Show Dashboard' to Continue"
                }
                is SetupStatus.Error -> {
                    statusView.text = "${it.errorMessage} ${it.error.localizedMessage}"
                }
                is SetupStatus.InProgress -> {
                    statusView.text = it.state
                }
            }
        })
        findViewById<Button>(R.id.connectButton)
                .setOnClickListener {
                    setupScreenVM.initConnection()
                }

        dashboardButton.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

}