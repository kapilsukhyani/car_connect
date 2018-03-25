package com.exp.carconnect.base.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.SplashScreenState
import com.exp.carconnect.base.viewmodel.SplashVM

class SplashView : Fragment() {
    lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewModelProviders
                .of(this)
                .get(SplashVM::class.java)
                .getAppLoadingStateLiveData()
                .observe(this, Observer {
                    showStatus(it!!)
                })
    }

    private fun showStatus(it: SplashScreenState) {
        statusView.text = it.toString()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.view_splash, null)
        statusView = view.findViewById(R.id.status)
        return view
    }
}
