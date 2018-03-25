package com.exp.carconnect.app.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.exp.carconnect.app.R
import com.exp.carconnect.app.viewmodel.WindowVM
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.fragment.SplashView
import com.exp.carconnect.base.state.SplashScreen


class CarConnectWindow : AppCompatActivity() {

    lateinit var windowContainer: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.window)
        windowContainer = findViewById(R.id.window_container)
        ViewModelProviders.of(this)
                .get(WindowVM::class.java)
                .getCurrentViewLiveData()
                .observe(this, Observer {
                    showView(it!!)
                })

    }

    private fun showView(it: CarConnectView) {
        when (it) {
            is SplashScreen -> {
                replaceFragment(SplashView())
            }
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.window_container, fragment)
                .commitNow()
    }
}