package com.exp.carconnect.dashboard.fragment

import android.app.Application
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.base.*
import com.exp.carconnect.base.state.*
import com.exp.carconnect.dashboard.R
import com.exp.carconnect.dashboard.state.DashboardScreen
import com.exp.carconnect.dashboard.state.DashboardScreenState
import com.exp.carconnect.dashboard.view.Dashboard
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_dashboard.*

class DashboardView : Fragment(), BackInterceptor {
    lateinit var dashboard: Dashboard
    lateinit var dashboardVM: DashboardVM

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.view_dashboard, null)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        dashboardVM = ViewModelProviders.of(this).get(DashboardVM::class.java)
        dashboardVM.getScreenStateLiveData()
                .observe(this, Observer {
                    onNewState(it!!)
                })
        settings_icon.setOnClickListener {
            dashboardVM.onSettingsIconClicked()
        }
    }

    private fun onNewState(it: DashboardScreenState) {
        when (it) {
            is DashboardScreenState.ShowNewSnapshot -> {
                showNewSnapshot(it.vehicle, it.data)
            }
            is DashboardScreenState.ShowError -> {
                showError(it.error)
            }
        }


    }

    private fun showError(error: String) {

    }

    private fun showNewSnapshot(vehicle: Vehicle, dashboardData: VehicleData) {
        dashboard.currentSpeed = dashboardData.speed.getValueOrDefault(0.toFloat())
        dashboard.currentRPM = dashboardData.rpm.getValueOrDefault(0.toFloat())
        dashboard.vin = vehicle.vin
        dashboard.showIgnitionIcon = dashboardData.ignition.getValueOrDefault(false)
        dashboard.showCheckEngineLight = dashboardData.milStatus
                .getValueOrDefault(MILStatus.Off).let {
                    it != MILStatus.Off
                }
        dashboard.fuelPercentage = dashboardData.fuel.getValueOrDefault(0.toFloat())
        dashboard.currentAirIntakeTemp = dashboardData.currentAirIntakeTemp.getValueOrDefault(0.toFloat())
        dashboard.currentAmbientTemp = dashboardData.currentAmbientTemp.getValueOrDefault(0.toFloat())
    }

    private fun <T> UnAvailableAvailableData<T>.getValueOrDefault(default: T): T {
        return when (this) {
            is UnAvailableAvailableData.Available -> {
                this.data
            }
            else -> {
                default
            }
        }
    }

    private fun initView(view: View) {
        dashboard = view.findViewById(R.id.dashboard)
        dashboard.online = true
        dashboard.rpmDribbleEnabled = true
        dashboard.speedDribbleEnabled = true
        dashboard.showSideGauges = false
        dashboard.postDelayed(Runnable { dashboard.showSideGauges = true }, 500)
    }


    override fun interceptBack(): Boolean {
        dashboardVM.onBackPressed()
        return false
    }

}

class DashboardVM(app: Application) : AndroidViewModel(app) {
    private val dashboardViewLiveData = MutableLiveData<DashboardScreenState>()
    private val store = (app as BaseAppContract).store
    private val storeSubscription: CompositeDisposable = CompositeDisposable()

    init {
        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is DashboardScreen }
                .filter { it.getBaseAppState().activeSession is UnAvailableAvailableData.Available<ActiveSession> }
                .filter { it.getActiveSession().vehicle is LoadableState.Loaded }
                .map {
                    Pair((it.getActiveSession().vehicle as LoadableState.Loaded).savedState,
                            it.getActiveSession().currentVehicleData)
                }
                .distinctUntilChanged()
                .subscribe {

                    if (it.second is LoadableState.Loaded) {
                        dashboardViewLiveData.value = DashboardScreenState.ShowNewSnapshot(it.first,
                                (it.second as LoadableState.Loaded).savedState)

                    } else if (it.second is LoadableState.LoadingError) {
                        dashboardViewLiveData.value = DashboardScreenState
                                .ShowError(getApplication<Application>()
                                        .getString(R.string.data_load_error,
                                                (it.second as LoadableState.LoadingError<Throwable>).error.localizedMessage))
                    }
                })
    }

    override fun onCleared() {
        storeSubscription.dispose()
    }

    fun getScreenStateLiveData(): LiveData<DashboardScreenState> {
        return dashboardViewLiveData
    }

    fun onBackPressed() {
        store.dispatch(BaseAppAction.KillActiveSession)
    }

    fun onSettingsIconClicked() {
        store.dispatch(CommonAppAction.PushViewToBackStack(SettingsScreen(SettingsScreenState.ShowingSettings)))
    }
}