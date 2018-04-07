package com.exp.carconnect.app.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.view.View
import com.exp.carconnect.app.R
import com.exp.carconnect.app.viewmodel.WindowVM
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.fragment.DeviceConnectionView
import com.exp.carconnect.base.fragment.DeviceManagementView
import com.exp.carconnect.base.fragment.SplashView
import com.exp.carconnect.base.state.ConnectionScreen
import com.exp.carconnect.base.state.DeviceManagementScreen
import com.exp.carconnect.base.state.SplashScreen


class CarConnectWindow : AppCompatActivity() {

    private lateinit var windowContainer: View
    private lateinit var windowVM: WindowVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.window)
        windowContainer = findViewById(R.id.window_container)
        windowVM = ViewModelProviders.of(this)
                .get(WindowVM::class.java)
        windowVM.getCurrentViewLiveData()
                .observe(this, Observer {
                    showView(it)
                })

    }

    private fun showView(it: CarConnectView?) {
        when (it) {
            null -> {
                finish()
            }
            is SplashScreen -> {
                replaceFragment(SplashView())
            }

            is DeviceManagementScreen -> {
                replaceFragment(DeviceManagementView(), viewTransition = false)
//                replaceFragment(DeviceManagementView(),
//                        SplashView.finSharedElement(windowContainer),
//                        DeviceManagementView.getSharedElementTransitionName(), false)
            }

            is ConnectionScreen -> {
                replaceFragment(DeviceConnectionView())
            }
        }
    }


    private fun FragmentTransaction.addShareElementTransitionIfNotEmpty(sharedElement: View? = null,
                                                                        sharedElementTransitionName: String? = null): FragmentTransaction {
        return if (sharedElement != null && null != sharedElementTransitionName) {
            this.addSharedElement(sharedElement, sharedElementTransitionName)
        } else {
            return this
        }
    }

    private fun replaceFragment(fragment: Fragment,
                                sharedElement: View? = null,
                                sharedElementTransitionName: String? = null,
                                viewTransition: Boolean = true) {
        if (viewTransition) {
            fragment.enterTransition = Fade()
            fragment.exitTransition = Fade()
        }
        if (null != sharedElement && null != sharedElementTransitionName) {
            fragment.sharedElementEnterTransition = SharedElementTransition()
            fragment.sharedElementReturnTransition = SharedElementTransition()
        }
        supportFragmentManager
                .beginTransaction()
                .addShareElementTransitionIfNotEmpty(sharedElement, sharedElementTransitionName)
                .replace(R.id.window_container, fragment)
                .commitNow()
    }


    override fun onBackPressed() {
        windowVM.onBackPressed()
    }
}

class SharedElementTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(ChangeBounds())
                .addTransition(ChangeTransform())
    }
}

