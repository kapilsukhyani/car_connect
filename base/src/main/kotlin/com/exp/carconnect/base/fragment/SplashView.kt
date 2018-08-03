package com.exp.carconnect.base.fragment

import android.app.Application
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.base.*
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.CompositeDisposable

class SplashView : Fragment() {
    companion object {
        const val TAG = "SplashView"
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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.view_splash, null)
        appLogo = view.findViewById(R.id.app_logo)
        containerLayout = view.findViewById(R.id.container)
        constraintSet.clone(containerLayout)
        return view
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
                                return@map SplashScreenState.ShowLoadingError
                            }
                        }
                    }
                    val baseAppState = (it[BaseAppState.STATE_KEY] as LoadableState.Loaded<BaseAppState>).savedState
                    if(baseAppState.baseAppPersistedState
                                    .appSettings
                                    .usageReportingEnabled){
                        (app as BaseAppContract).enableReporting()
                    }
                    if (baseAppState.activeSession is UnAvailableAvailableData.Available) {
                        //moving background session to foreground
                        store.dispatch(BaseAppAction.StopBackgroundSession)
                        (app as BaseAppContract).onDataLoadingStartedFor((baseAppState.activeSession.data.vehicle as LoadableState.Loaded).savedState)
                    } else {
                        store.dispatch(CommonAppAction.ReplaceViewOnBackStackTop(DeviceManagementScreen(DeviceManagementScreenState.LoadingDevices)))
                    }
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