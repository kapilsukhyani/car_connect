package com.exp.carconnect.base.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Application
import android.arch.lifecycle.*
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.R
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.base.state.SplashScreenState
import io.reactivex.disposables.CompositeDisposable

class SplashView : Fragment() {
    companion object {
        const val TAG = "SplashView"

        fun finSharedElement(windowContainer: View): View {
            return windowContainer.findViewById<View>(R.id.app_logo)
        }
    }

    private lateinit var appLogo: View
    private val constraintSet = ConstraintSet()
    private lateinit var containerLayout: ConstraintLayout
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
        Log.d(TAG, "Received status $it")
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.view_splash, null)
        appLogo = view.findViewById(R.id.app_logo)
        containerLayout = view.findViewById(R.id.container)
        constraintSet.clone(containerLayout)
        return view
    }



    fun animateView() {
        val animator = ValueAnimator.ofFloat(.95f, .45f)
        animator.startDelay = 1000
        animator.addUpdateListener { it ->
            constraintSet.setVerticalBias(R.id.app_logo, it.animatedValue as Float)
            constraintSet.applyTo(containerLayout)
        }
        animator.interpolator = FastOutSlowInInterpolator()
        animator.duration = 600
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                appLogo.animate()
                        .rotationBy(-90f)
                        .translationXBy(-30f)
                        .translationYBy(-70f)
                        .start()
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })

        animator.start()
    }

    fun getSharedElement(): View {
        return appLogo
    }

}

class SplashVM(app: Application) : AndroidViewModel(app) {

    private val splashScreenStateLiveData: MutableLiveData<SplashScreenState> = MutableLiveData()
    private val stateSubscription: CompositeDisposable = CompositeDisposable()

    private val store = (app as BaseAppContract).store

    init {
        stateSubscription.add(store
                .asCustomObservable()
                .map { it.moduleStateMap }
                .distinctUntilChanged()
                .map {
                    it.forEach { entry ->
                        when (entry.value) {
                            LoadableState.Loading,
                            LoadableState.NotLoaded -> {
                                return@map SplashScreenState.LoadingAppState
                            }
                            is LoadableState.LoadingError -> {
                                return@map SplashScreenState.ShowingLoadingError
                            }
                        }
                    }
                    store.dispatch(CommonAppAction.AppStateLoaded)
                    return@map SplashScreenState.LoadingAppState
                }
                .distinctUntilChanged()
                .subscribe(
                        {
                            splashScreenStateLiveData.value = it

                        }, {
                    //todo log error
                }
                ))


    }

    fun getAppLoadingStateLiveData(): LiveData<SplashScreenState> {
        return splashScreenStateLiveData
    }

    override fun onCleared() {
        stateSubscription.dispose()
    }
}