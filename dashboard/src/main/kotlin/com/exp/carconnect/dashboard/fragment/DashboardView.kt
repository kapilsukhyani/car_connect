package com.exp.carconnect.dashboard.fragment

import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.*
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.crashlytics.android.answers.CustomEvent
import com.exp.carconnect.base.*
import com.exp.carconnect.base.state.*
import com.exp.carconnect.dashboard.R
import com.exp.carconnect.dashboard.state.DashboardAction
import com.exp.carconnect.dashboard.state.DashboardScreen
import com.exp.carconnect.dashboard.state.DashboardScreenState
import com.exp.carconnect.dashboard.view.DashboardViewGroup
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_dashboard.*
import redux.api.Reducer


class DashboardView : Fragment(), BackInterceptor {
    private lateinit var dashboard: DashboardViewGroup
    private lateinit var dashboardVM: DashboardVM
    private var setVehicleInfo = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.view_dashboard_v2, null)
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
                showNewSnapshot(it.vehicle, it.data, it.theme)
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
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    dashboardVM.onDataErrorAcknowledged()
                }
                .create()
                .show()
    }

    private fun showNewSnapshot(vehicle: Vehicle, dashboardData: LiveVehicleData, theme: DashboardTheme) {
        if (theme == DashboardTheme.Light) {
            dashboard.theme = DashboardViewGroup.Theme.Light
        } else {
            dashboard.theme = DashboardViewGroup.Theme.Dark
        }
        dashboard.currentSpeed = dashboardData.speed.getValueOrDefault(0.toFloat())
        dashboard.vin = vehicle.vin
        dashboard.showIgnitionIcon = dashboardData.ignition.getValueOrDefault(false)
        dashboard.showCheckEngineLight = dashboardData.milStatus
                .getValueOrDefault(MILStatus.Off).let {
                    it != MILStatus.Off
                }

        //landscape only data
        if (view?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dashboard.currentRPM = dashboardData.rpm.getValueOrDefault(0.toFloat())
            dashboard.fuelPercentage = dashboardData.fuel.getValueOrDefault(0.toFloat())
            dashboard.currentAirIntakeTemp = dashboardData.currentAirIntakeTemp.getValueOrDefault(0.toFloat())
            dashboard.currentAmbientTemp = dashboardData.currentAmbientTemp.getValueOrDefault(0.toFloat())
        }

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
        if (view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dashboard.showSideGauges = false
            dashboard.postDelayed({ dashboard.showSideGauges = true }, 500)
        }
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

    data class DashboardState(val vehicle: Vehicle, val liveVehicleData: LoadableState<LiveVehicleData, Throwable>, val theme: DashboardTheme)

    init {
        app.logContentViewEvent("DashboardView")
        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is DashboardScreen && it.isVehicleInfoLoaded() }
                .map {
                    DashboardState((it.getActiveSession().vehicle as LoadableState.Loaded).savedState,
                            it.getActiveSession().liveVehicleData, it.getDashboardTheme())

                }
                .distinctUntilChanged()
                .subscribe {
                    if (it.liveVehicleData is LoadableState.Loaded) {
                        store.dispatch(DashboardAction.AddNewDashboardState(DashboardScreenState.ShowNewSnapshot(it.vehicle,
                                (it.liveVehicleData).savedState, it.theme)))

                    } else if (it.liveVehicleData is LoadableState.LoadingError) {
                        store.dispatch(DashboardAction.AddNewDashboardState(DashboardScreenState
                                .ShowError(getApplication<Application>()
                                        .getString(R.string.data_load_error,
                                                (it.liveVehicleData).error.localizedMessage))))
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
        store.dispatch(BaseAppAction.StartBackgroundOrKillActiveSession)
    }

    fun onSettingsIconClicked() {
        getApplication<Application>().logSettingsClickedEvent("DashboardView")
        store.dispatch(CommonAppAction.PushViewToBackStack(SettingsScreen(SettingsScreenState.ShowingSettings)))
    }

    fun onReportIconClicked() {
        getApplication<Application>().logEvent(CustomEvent("report_icon_clicked")
                .putCustomAttribute("location", "DashboardView"))
        (getApplication<Application>() as BaseAppContract).onReportRequested()
    }

    fun onDataErrorAcknowledged() {
        getApplication<Application>().logDataErrorAcknowledgedEvent("DashboardView")
        store.dispatch(CommonAppAction.FinishCurrentView)
    }
}

class DashboardScreenStateReducer : Reducer<AppState> {
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