package com.exp.carconnect.dashboard.fragment

import android.app.AlertDialog
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
import com.exp.carconnect.dashboard.state.DashboardAction
import com.exp.carconnect.dashboard.state.DashboardScreen
import com.exp.carconnect.dashboard.state.DashboardScreenState
import com.exp.carconnect.dashboard.view.Dashboard
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_dashboard.*
import redux.api.Reducer


class DashboardView : Fragment(), BackInterceptor {
    lateinit var dashboard: Dashboard
    lateinit var dashboardVM: DashboardVM
    var setVehicleInfo = false

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

        report_icon.setOnClickListener {
            dashboardVM.onReportIconClicked()
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
        AlertDialog
                .Builder(activity)
                .setTitle(getString(com.exp.carconnect.base.R.string.data_loading_error))
                .setMessage(error)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    dashboardVM.onDataErrorAcknowledged()
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun showNewSnapshot(vehicle: Vehicle, dashboardData: LiveVehicleData) {
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

        if (!setVehicleInfo) {
            if (vehicle.attributes is UnAvailableAvailableData.Available) {
                val attributes = vehicle.attributes as UnAvailableAvailableData.Available
                vehicle_info.text = getString(R.string.vehicle_info_text, attributes.data.make, attributes.data.model, attributes.data.modelYear)
            } else {
                vehicle_info.text = ""
            }
            setVehicleInfo = true
        }
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
                            it.getActiveSession().liveVehicleData)
                }
                .distinctUntilChanged()
                .subscribe {
                    if (it.second is LoadableState.Loaded) {
                        store.dispatch(DashboardAction.AddNewDashboardState(DashboardScreenState.ShowNewSnapshot(it.first,
                                (it.second as LoadableState.Loaded).savedState)))

                    } else if (it.second is LoadableState.LoadingError) {
                        store.dispatch(DashboardAction.AddNewDashboardState(DashboardScreenState
                                .ShowError(getApplication<Application>()
                                        .getString(R.string.data_load_error,
                                                (it.second as LoadableState.LoadingError<Throwable>).error.localizedMessage))))
                    }
                })

        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is DashboardScreen }
                .map { (it.uiState.currentView as DashboardScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    dashboardViewLiveData.value = it
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

    fun onReportIconClicked() {
        (getApplication<Application>() as BaseAppContract).onReportRequested()
    }

    fun onDataErrorAcknowledged() {
        store.dispatch(CommonAppAction.FinishCurrentView)
    }
}

class DashboardReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is DashboardAction.AddNewDashboardState -> {
                updateState(state, action.state)
            }
            else -> {
                state
            }
        }
    }

    private fun updateState(state: AppState, dashboardScreenState: DashboardScreenState): AppState {
        return state.copy(uiState = state
                .uiState
                .copy(backStack = state
                        .uiState
                        .backStack
                        .subList(0, state.uiState.backStack.size - 1) +
                        DashboardScreen(dashboardScreenState)))
    }

}