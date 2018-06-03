package com.exp.carconnect.base.fragment

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager
import android.view.*
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.Frequency
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import kotlinx.android.synthetic.main.view_settings.*
import java.util.concurrent.TimeUnit

class SettingsView : Fragment() {

    companion object {
        const val TAG = "PreferenceSettingsTag"
    }

    lateinit var settingVM: SettingsVM

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


    init {
        defaultSharedPref.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        defaultSharedPref
                .unregisterOnSharedPreferenceChangeListener(this)
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