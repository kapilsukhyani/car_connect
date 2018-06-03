package com.exp.carconnect.base.fragment

import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.arch.lifecycle.*
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager
import android.view.*
import com.exp.carconnect.base.*
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_settings.*
import redux.api.Reducer
import java.util.concurrent.TimeUnit

class SettingsView : Fragment() {

    companion object {
        const val TAG = "PreferenceSettingsTag"
    }

    lateinit var settingVM: SettingsVM

    var progressDialog: ProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.view_settings, null)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingVM = ViewModelProviders.of(this).get(SettingsVM::class.java)
        childFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, Settings())
                .commitNow()
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val animator = ViewAnimationUtils.createCircularReveal(settings_view,
                        settings_view.width, 0,
                        0f,
                        Math.hypot(settings_view.width.toDouble(), settings_view.height.toDouble()).toFloat())


                settings_view.visibility = View.VISIBLE
                animator.duration = 400
                animator.start()
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        clear_dtc_button.setOnClickListener {
            settingVM.clearDtcs()
        }

        settingVM.getSettingsScreenStateLiveData().observe(this, Observer {
            onNewState(it!!)
        })
    }

    private fun onNewState(newState: SettingsScreenState) {
        println("Settings New State $newState")
        when (newState) {
            SettingsScreenState.ShowingClearDTCButton -> {
                clear_dtc_button.visibility = View.VISIBLE
            }

            SettingsScreenState.HidingClearDTCButton -> {
                clear_dtc_button.visibility = View.GONE
            }

            SettingsScreenState.ShowingClearingDTCsProgress -> {
                showClearingDTCProgress()
            }

            SettingsScreenState.ClearingDtcsFailed -> {
                hideProgressDialog()
                showCLearDTCError()
            }

            SettingsScreenState.ClearingDTCsSuccessful -> {
                hideProgressDialog()
            }
        }
    }


    private fun showClearingDTCProgress() {
        progressDialog = ProgressDialog.show(activity, getString(R.string.please_wait), getString(R.string.clearing_dtcs))
        progressDialog?.show()

    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }

    private fun showCLearDTCError() {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.operation_failed))
                .setMessage(getString(R.string.clear_dtc_error_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    dialog.dismiss()
                }
                .create()
                .show()
    }

}


class Settings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

}

class SettingsVM(app: Application) : AndroidViewModel(app), SharedPreferences.OnSharedPreferenceChangeListener {
    private val app = getApplication<Application>()
    private val store = (app as BaseAppContract).store
    private val defaultSharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
    private val subscriptions: CompositeDisposable = CompositeDisposable()
    private val settingsScreenStateLiveData: MutableLiveData<SettingsScreenState> = MutableLiveData()


    init {
        defaultSharedPref.registerOnSharedPreferenceChangeListener(this)

        val screenStateSubscription = store.asCustomObservable()
                .map { (it.uiState.currentView as SettingsScreen).screenState }
                .distinctUntilChanged()
                .subscribe { settingsScreenStateLiveData.value = it}
        subscriptions.add(screenStateSubscription)

        listenForActiveSessionMilStatus()
    }

    override fun onCleared() {
        defaultSharedPref
                .unregisterOnSharedPreferenceChangeListener(this)
        subscriptions.dispose()
    }


    private fun listenForActiveSessionMilStatus() {
        val subscription = store.asCustomObservable()
                .filter { it.isActiveVehicleMilStatusLoaded() }
                .map { it.getMilStatus() }
                .distinctUntilChanged()
                .subscribe {
                    if (it is MILStatus.On) {
                        store.dispatch(SettingsViewAction.ShowClearDTCButton)
                    } else {
                        store.dispatch(SettingsViewAction.HideClearDTCButton)
                    }
                }
        subscriptions.add(subscription)
    }


    fun getSettingsScreenStateLiveData(): LiveData<SettingsScreenState> {
        return settingsScreenStateLiveData
    }

    fun clearDtcs() {
        val subscription = store.asCustomObservable()
                .filter { it.isAnActiveSessionAvailable() }
                .map { it.getActiveSession().clearDTCsOperationState }
                .filter { it != ClearDTCOperationState.None }
                .distinctUntilChanged()
                .subscribe {
                    when (it) {
                        is ClearDTCOperationState.Clearing -> {
                            store.dispatch(SettingsViewAction.ShowClearingDTCInProgressState)
                        }

                        is ClearDTCOperationState.Error -> {
                            store.dispatch(SettingsViewAction.ShowClearDTCFailedErrorState)
                            store.dispatch(BaseAppAction.UpdateClearDTCsOperationStateToNone)
                        }

                        is ClearDTCOperationState.Successful -> {
                            store.dispatch(SettingsViewAction.ShowClearDTCSuccessfulState)
                            store.dispatch(BaseAppAction.UpdateClearDTCsOperationStateToNone)
                        }
                    }
                }
        subscriptions.add(subscription)
        store.dispatch(BaseAppAction.ClearDTCs)

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val appSettings = store.state.getBaseAppState().baseAppPersistedState.appSettings
        var updatedSettings: AppSettings? = null
        when (key) {
            app.getString(R.string.background_connection_pref_key) -> {
                updatedSettings = appSettings
                        .copy(backgroundConnectionEnabled = defaultSharedPref
                                .getBoolean(key, AppSettings.DEFAULT_BACKGROND_OPERATION_ENABLED))
            }
            app.getString(R.string.auto_connect_pref_key) -> {
                updatedSettings = appSettings
                        .copy(autoConnectToLastConnectedDongleOnLaunch = defaultSharedPref
                                .getBoolean(key, AppSettings.DEFAULT_AUTO_CONNECTED_ENABLED))
            }
            app.getString(R.string.unit_system_pref_key) -> {
                val unitSystem = if (defaultSharedPref.getString(key, app.getString(R.string.matrix))
                        == app.getString(R.string.matrix)) {
                    UnitSystem.Matrix
                } else {
                    UnitSystem.Imperial
                }
                updatedSettings = appSettings.copy(dataSettings = appSettings.dataSettings.copy(unitSystem = unitSystem))
            }
            app.getString(R.string.fuel_level_pull_frequency_pref_key) -> {
                val fuelLevelPullFrequency = defaultSharedPref.getString(key, DataSettings.DEFAULT_FUEL_LEVEL_REFRESH_FREQUENCY.toString()).toLong()
                updatedSettings = appSettings.copy(dataSettings = appSettings
                        .dataSettings.copy(fuelLevelRefreshFrequency = Frequency(fuelLevelPullFrequency, TimeUnit.MINUTES)))
            }

            app.getString(R.string.fast_changing_data_pull_frequency_pref_key) -> {
                val fastChangingDataFrequency = defaultSharedPref.getString(key, DataSettings.DEFAULT_FAST_CHANGING_DATA_REFRESH_FREQUENCY.toString()).toLong()
                updatedSettings = appSettings.copy(dataSettings = appSettings
                        .dataSettings.copy(fastChangingDataRefreshFrequency = Frequency(fastChangingDataFrequency, TimeUnit.MILLISECONDS)))

            }
            app.getString(R.string.temperature_pull_frequency_pref_key) -> {
                val temperaturePullFrequency = defaultSharedPref.getString(key, DataSettings.DEFAULT_TEMPERATURE_REFRESH_FREQUENCY.toString()).toLong()
                updatedSettings = appSettings.copy(dataSettings = appSettings
                        .dataSettings.copy(temperatureRefreshFrequency = Frequency(temperaturePullFrequency, TimeUnit.MILLISECONDS)))

            }
            app.getString(R.string.pressure_pull_frequency_pref_key) -> {
                val pressurePullFrequency = defaultSharedPref.getString(key, DataSettings.DEFAULT_PRESSURE_REFRESH_FREQUENCY.toString()).toLong()
                updatedSettings = appSettings.copy(dataSettings = appSettings
                        .dataSettings.copy(pressureRefreshFrequency = Frequency(pressurePullFrequency, TimeUnit.MINUTES)))
            }
            app.getString(R.string.fuel_level_notification_pref_key) -> {
                updatedSettings = if (defaultSharedPref.getBoolean(key, true)) {
                    appSettings.copy(notificationSettings = appSettings.notificationSettings.copy(fuelNotificationSettings = FuelNotificationSettings.On(FuelNotificationSettings.DEFAULT_FUEL_PERCENTAGE_LEVEL.toFloat() / 100)))
                } else {
                    appSettings.copy(notificationSettings = appSettings.notificationSettings.copy(fuelNotificationSettings = FuelNotificationSettings.Off))
                }


            }
            app.getString(R.string.fuel_level_notification_threshold_pref_key) -> {
                val fuelLevelNotificationThreshold = defaultSharedPref.getString(key, FuelNotificationSettings.DEFAULT_FUEL_PERCENTAGE_LEVEL.toString()).toFloat() / 100
                updatedSettings = appSettings.copy(notificationSettings = appSettings.notificationSettings.copy(fuelNotificationSettings = FuelNotificationSettings.On(fuelLevelNotificationThreshold)))
            }

            app.getString(R.string.speed_notifications_pref_key) -> {
                updatedSettings = if (defaultSharedPref.getBoolean(key, true)) {
                    appSettings.copy(notificationSettings = appSettings.notificationSettings.copy(speedNotificationSettings = SpeedNotificationSettings.On(SpeedNotificationSettings.DEFAULT_MAX_SPEED_THRESHOLD)))
                } else {
                    appSettings.copy(notificationSettings = appSettings.notificationSettings.copy(speedNotificationSettings = SpeedNotificationSettings.Off))
                }
            }
            app.getString(R.string.speed_notification_threshold_pref_key) -> {
                val speedNotificationThreshold = defaultSharedPref.getString(key, SpeedNotificationSettings.DEFAULT_MAX_SPEED_THRESHOLD.toString()).toInt()
                updatedSettings = appSettings.copy(notificationSettings = appSettings.notificationSettings.copy(speedNotificationSettings = SpeedNotificationSettings.On(speedNotificationThreshold)))

            }
            app.getString(R.string.dashboard_settings_pref_key) -> {
                val theme = if (defaultSharedPref.getString(key, app.getString(R.string.dark_dashboard_theme))
                        == app.getString(R.string.dark_dashboard_theme)) {
                    DashboardTheme.Dark
                } else {
                    DashboardTheme.Light
                }
                updatedSettings = appSettings.copy(displaySettings = appSettings.displaySettings.copy(dashboardTheme = theme))
            }

        }

        updatedSettings?.let {
            store.dispatch(BaseAppAction.UpdateAppSettings(it))
            store.dispatch(BaseAppAction.RefreshActiveSessionDataFetchRate(it))
        }

    }
}

private sealed class SettingsViewAction {
    object ShowClearDTCButton : SettingsViewAction()
    object HideClearDTCButton : SettingsViewAction()
    object ShowClearingDTCInProgressState : SettingsViewAction()
    object ShowClearDTCSuccessfulState : SettingsViewAction()
    object ShowClearDTCFailedErrorState : SettingsViewAction()
}

class SettingsScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            SettingsViewAction.ShowClearDTCButton -> {
                updateSettingsScreenUIStateTo(state, SettingsScreenState.ShowingClearDTCButton)
            }
            SettingsViewAction.HideClearDTCButton -> {
                updateSettingsScreenUIStateTo(state, SettingsScreenState.HidingClearDTCButton)
            }
            SettingsViewAction.ShowClearingDTCInProgressState -> {
                updateSettingsScreenUIStateTo(state, SettingsScreenState.ShowingClearingDTCsProgress)
            }
            SettingsViewAction.ShowClearDTCSuccessfulState -> {
                updateSettingsScreenUIStateTo(state, SettingsScreenState.ClearingDTCsSuccessful)
            }
            SettingsViewAction.ShowClearDTCFailedErrorState -> {
                updateSettingsScreenUIStateTo(state, SettingsScreenState.ClearingDtcsFailed)
            }
            else -> {
                state
            }
        }
    }

    private fun updateSettingsScreenUIStateTo(state: AppState, newState: SettingsScreenState): AppState {
        return state.copy(uiState = state
                .uiState
                .copy(backStack = state
                        .uiState
                        .backStack
                        .subList(0, state.uiState.backStack.size - 1) +
                        SettingsScreen(newState)))
    }


}