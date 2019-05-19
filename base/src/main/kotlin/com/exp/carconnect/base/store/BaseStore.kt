package com.exp.carconnect.base.store

import android.annotation.SuppressLint
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.exp.carconnect.base.*
import com.exp.carconnect.base.state.*
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import redux.api.Store
import redux.asObservable
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class PersistedAppState(val knownDongles: Set<Dongle> = hashSetOf(),
                             val knownVehicles: Set<Vehicle> = hashSetOf(),
                             val lastConnectedDongle: Dongle? = null,
                             val lastConnectedVehicle: Vehicle? = null,
                             val appSettings: AppSettings = AppSettings())


class BaseStore(val context: Context, val ioScheduler: Scheduler) {
    companion object {
        const val TAG = "BaseStore"
    }

    class StateListener(val store: Store<AppState>,
                        private val ioScheduler: Scheduler,
                        private val baseAppStateDao: BaseAppStateDao,
                        private val context: Context) {

        private val activeSessionObservable = store.asObservable()
                .filter { it.isBaseStateLoaded() }
                .filter { it.getBaseAppState().activeSession is UnAvailableAvailableData.Available }
                .map { (it.getBaseAppState().activeSession as UnAvailableAvailableData.Available).data }

        private val recentlyUsedDongleObservable = activeSessionObservable
                .map { it.dongle }
                .distinctUntilChanged()
                .doOnNext {
                    store.dispatch(BaseAppAction.AddNewConnectedDongle(it))
                }
                .observeOn(ioScheduler)

        private val recentlyUsedVehicleObservable = activeSessionObservable
                .filter { it.vehicle is LoadableState.Loaded }
                .map { (it.vehicle as LoadableState.Loaded).savedState }
                .distinctUntilChanged()
                .doOnNext {
                    store.dispatch(BaseAppAction.AddNewVehicle(it))
                }
                .observeOn(ioScheduler)

        private val privacyPolicyAcceptedObservable =  store.asObservable()
                .filter { it.isBaseStateLoaded() }
                .map { it.getBaseAppState().baseAppPersistedState.appSettings.privacyPolicyAccepted }
                .distinctUntilChanged()
                .filter { it }

        init {
            startListening()
        }

        private fun startListening() {
            recentlyUsedDongleObservable
                    .subscribe {
                        Timber.d("$TAG persisting new dongle")
                        baseAppStateDao.insertDongleAsRecentlyUsed(it.toEntity())
                    }
            recentlyUsedVehicleObservable
                    .subscribe {
                        Timber.d("$TAG persisting new vehicle")
                        baseAppStateDao.insertVehicleAsRecentlyUsed(it.toEntity())
                    }
            privacyPolicyAcceptedObservable.subscribe{
                Timber.d("$TAG persisting privacy policy accepted")
                baseAppStateDao.updatePrivacyPolicyToRead(context)
            }
        }
    }

    private val db: CarConnectBaseDB = Room
            .databaseBuilder(context, CarConnectBaseDB::class.java, "CarConnectBaseDB")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Timber.d("[${BaseStore.TAG}] DB created")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Timber.d("[${BaseStore.TAG}] DB opened")
                }
            })
            .build()

    private val baseAppStateDao = db.baseAppStateDao()


    private val loadAllDongles = baseAppStateDao
            .getAllDonglesAndComplete()
            .onErrorReturn { listOf() }
            .map {
                Pair(it.firstOrNull { it.recentlyUsed }?.toDongle(),
                        it.map { it.toDongle() }.toHashSet())
            }


    private val loadAllVehicles = baseAppStateDao
            .getAllVehiclesAndComplete()
            .onErrorReturn { listOf() }
            .map {
                Pair(it.firstOrNull { it.recentlyUsed }?.toVehicle(),
                        it.map { it.toVehicle() }.toHashSet())
            }


    init {
        db.openHelper.writableDatabase
    }

    fun startListening(store: Store<AppState>) {
        StateListener(store, ioScheduler, baseAppStateDao, context)
    }

    internal fun loadAppState(): Single<PersistedAppState> {
        return loadAllDongles
                .zipWith(loadAllVehicles, BiFunction<Pair<Dongle?, HashSet<Dongle>>,
                        Pair<Vehicle?, HashSet<Vehicle>>, PersistedAppState> { t1, t2 ->
                    PersistedAppState(knownDongles = t1.second,
                            knownVehicles = t2.second,
                            lastConnectedDongle = t1.first,
                            lastConnectedVehicle = t2.first)
                })
                .map {

                    val defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context)

                    val privacyPolicyAccepted = defaultSharedPref
                            .getBoolean(context.getString(R.string.privacy_policy_flag_key), AppSettings.DEFAULT_PRIVACY_POLICY_FLAG_VALUE)
                    val backgroundConnectionEnabled = defaultSharedPref
                            .getBoolean(context.getString(R.string.background_connection_pref_key), AppSettings.DEFAULT_BACKGROUND_OPERATION_ENABLED)
                    val autoConnectEnabled = defaultSharedPref
                            .getBoolean(context.getString(R.string.auto_connect_pref_key), AppSettings.DEFAULT_AUTO_CONNECTED_ENABLED)

                    val unitSystem = if (defaultSharedPref.getString(context.getString(R.string.unit_system_pref_key), context.getString(R.string.matrix))
                            == context.getString(R.string.matrix)) {
                        UnitSystem.Matrix
                    } else {
                        UnitSystem.Imperial
                    }
                    val fuelLevelPullFrequency = Frequency(defaultSharedPref
                            .getString(context.getString(R.string.fuel_level_pull_frequency_pref_key), DataSettings.DEFAULT_FUEL_LEVEL_REFRESH_FREQUENCY.toString()).toLong(), TimeUnit.MINUTES)
                    val fastChangingDataFrequency = Frequency(defaultSharedPref
                            .getString(context.getString(R.string.fast_changing_data_pull_frequency_pref_key), DataSettings.DEFAULT_FAST_CHANGING_DATA_REFRESH_FREQUENCY.toString()).toLong(), TimeUnit.MILLISECONDS)
                    val temperaturePullFrequency = Frequency(defaultSharedPref
                            .getString(context.getString(R.string.temperature_pull_frequency_pref_key), DataSettings.DEFAULT_TEMPERATURE_REFRESH_FREQUENCY.toString()).toLong(), TimeUnit.MILLISECONDS)
                    val pressurePullFrequency = Frequency(defaultSharedPref
                            .getString(context.getString(R.string.pressure_pull_frequency_pref_key), DataSettings.DEFAULT_PRESSURE_REFRESH_FREQUENCY.toString()).toLong(), TimeUnit.MINUTES)

                    val fuelLevelNotificationSettings =
                            if (defaultSharedPref.getBoolean(context.getString(R.string.fuel_level_notification_pref_key), true)) {
                                FuelNotificationSettings.On(defaultSharedPref.getString(context.getString(R.string.fuel_level_notification_threshold_pref_key),
                                        FuelNotificationSettings.DEFAULT_FUEL_PERCENTAGE_LEVEL.toString()).toFloat() / 100)
                            } else {
                                FuelNotificationSettings.Off
                            }

                    val speedNotificationSettings =
                            if (defaultSharedPref.getBoolean(context.getString(R.string.speed_notifications_pref_key), true)) {
                                SpeedNotificationSettings.On(defaultSharedPref.getString(context.getString(R.string.speed_notification_threshold_pref_key), SpeedNotificationSettings.DEFAULT_MAX_SPEED_THRESHOLD.toString()).toInt())
                            } else {
                                SpeedNotificationSettings.Off
                            }

                    val theme = if (defaultSharedPref.getString(context.getString(R.string.dashboard_settings_pref_key), context.getString(R.string.dark_dashboard_theme))
                            == context.getString(R.string.dark_dashboard_theme)) {
                        DashboardTheme.Dark
                    } else {
                        DashboardTheme.Light
                    }

                    it.copy(appSettings = AppSettings(dataSettings = DataSettings(unitSystem = unitSystem,
                            fuelLevelRefreshFrequency = fuelLevelPullFrequency,
                            fastChangingDataRefreshFrequency = fastChangingDataFrequency,
                            temperatureRefreshFrequency = temperaturePullFrequency,
                            pressureRefreshFrequency = pressurePullFrequency),
                            notificationSettings = NotificationSettings(fuelNotificationSettings = fuelLevelNotificationSettings,
                                    speedNotificationSettings = speedNotificationSettings),
                            displaySettings = DisplaySettings(dashboardTheme = theme),
                            backgroundConnectionEnabled = backgroundConnectionEnabled,
                            autoConnectToLastConnectedDongleOnLaunch = autoConnectEnabled,
                            privacyPolicyAccepted = privacyPolicyAccepted))

                }
                .onErrorReturn {
                    Crashlytics.getInstance().core.logException(Exception("Unable to restore state", it))
                    Timber.d(it, "[${BaseStore.TAG}] unexpected error while loading state")
                    PersistedAppState()
                }

    }

}

@SuppressLint("ApplySharedPref")
fun BaseAppStateDao.updatePrivacyPolicyToRead(context: Context) {
    val defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    defaultSharedPref.edit().putBoolean(context.getString(R.string.privacy_policy_flag_key), true).commit()
}