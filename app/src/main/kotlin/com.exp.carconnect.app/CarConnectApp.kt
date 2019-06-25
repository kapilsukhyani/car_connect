package com.exp.carconnect.app

import android.app.Application
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.*
import com.exp.carconnect.app.state.*
import com.exp.carconnect.base.*
import com.exp.carconnect.base.fragment.DeviceConnectionScreenStateReducer
import com.exp.carconnect.base.fragment.DeviceManagementScreenStateReducer
import com.exp.carconnect.base.fragment.SettingsScreenStateReducer
import com.exp.carconnect.base.network.VehicleInfoLoaderFactoryImpl
import com.exp.carconnect.base.notification.Notifier
import com.exp.carconnect.base.state.*
import com.exp.carconnect.base.store.BaseStore
import com.exp.carconnect.dashboard.DashboardAppContract
import com.exp.carconnect.dashboard.fragment.DashboardScreenStateReducer
import com.exp.carconnect.dashboard.state.DashboardScreen
import com.exp.carconnect.dashboard.state.DashboardScreenState
import com.exp.carconnect.donation.DonationAppContract
import com.exp.carconnect.donation.fragment.DonationScreenStateReducer
import com.exp.carconnect.donation.state.DonationModuleState
import com.exp.carconnect.donation.state.DonationModuleStateReducer
import com.exp.carconnect.donation.state.DonationStateLoadingEpic
import com.exp.carconnect.donation.state.UpdateDonationEpic
import com.exp.carconnect.donation.store.DonationStore
import com.exp.carconnect.donation.store.DonationStoreImpl
import com.exp.carconnect.obdlib.OBDLogger
import com.exp.carconnect.report.fragment.ReportScreenStateReducer
import com.exp.carconnect.report.pdf.ReportPDFGenerator
import com.google.gson.Gson
import com.kobakei.ratethisapp.RateThisApp
import io.fabric.sdk.android.Fabric
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import redux.api.Store
import redux.applyMiddleware
import redux.combineReducers
import redux.createStore
import redux.observable.createEpicMiddleware
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


//https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html#foreground.type=image&foreground.space.trim=1&foreground.space.pad=-0.05&foreColor=rgba(96%2C%20125%2C%20139%2C%200)&backColor=rgb(255%2C%20255%2C%20255)&crop=0&backgroundShape=circle&effects=score&name=ic_launcher
//https://sqliteonline.com/
//http://randomvin.com/

class CarConnectApp : Application(),
        BaseAppContract,
        DashboardAppContract,
        DonationAppContract {
    override val mainScheduler: Scheduler = AndroidSchedulers.mainThread()
    override val ioScheduler: Scheduler = Schedulers.io()
    override val computationScheduler: Scheduler = Schedulers.computation()
    private var reportingEnabled = AtomicBoolean(false)

    companion object {
        const val TAG = "CarConnectApp"
    }

    override lateinit var store: Store<AppState>
    override lateinit var persistenceStore: BaseStore
    override lateinit var donationStore: DonationStore

    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        configureSeekRatingModule()

        Timber.plant(Timber.DebugTree())
        OBDLogger.init(object : OBDLogger {
            override fun log(tag: String, message: String) {
                Timber.d("[OBD_LAYER][$tag] $message")
            }

            override fun log(tag: String, exception: Throwable) {
                Timber.d(exception, "[OBD_LAYER][$tag]")
            }

            override fun log(tag: String, message: String, exception: Throwable) {
                Timber.d(exception, "[OBD_LAYER][$tag] $message")
            }

        })

        val initStartTime = System.currentTimeMillis()
        persistenceStore = BaseStore(this, ioScheduler)
        donationStore = DonationStoreImpl(PreferenceManager.getDefaultSharedPreferences(this), gson)

        val sessionManager = OBDDeviceSessionManager(this,
                ioScheduler,
                computationScheduler,
                mainScheduler,
                VehicleInfoLoaderFactoryImpl().getVehicleInfoLoader())

        val reducers = combineReducers(AppStateNavigationReducer(),
                BaseAppStateReducer(),
                ActiveSessionReducer(),
                ReportModuleReducer(),
                DeviceManagementScreenStateReducer(),
                DeviceConnectionScreenStateReducer(this),
                DashboardScreenStateReducer(),
                ReportScreenStateReducer(),
                SettingsScreenStateReducer(),
                DonationModuleStateReducer(),
                DonationScreenStateReducer())

        val initialState = AppState(mapOf(Pair(BaseAppState.STATE_KEY, LoadableState.NotLoaded),
                Pair(DonationModuleState.DONATION_STATE_KEY, LoadableState.NotLoaded)),
                CarConnectUIState(Stack()))

        val appStateLoadingMiddleware = createEpicMiddleware(BaseSateLoadingEpic(ioScheduler,
                mainScheduler,
                this))
        val donationLoadingMiddleWare = createEpicMiddleware(DonationStateLoadingEpic(ioScheduler,
                mainScheduler,
                donationStore))
        val obdSessionManagementMiddleware = createEpicMiddleware(OBDSessionManagementEpic(ioScheduler,
                mainScheduler,
                sessionManager))
        val clearDTCsMiddleware = createEpicMiddleware(ClearDTCsEpic(ioScheduler,
                mainScheduler,
                sessionManager))
        val fetchReportMiddleWare = createEpicMiddleware(FetchReportEpic(ioScheduler,
                mainScheduler,
                sessionManager))
        val captureReportMiddleWare = createEpicMiddleware(CaptureReportEpic(ioScheduler,
                mainScheduler,
                ReportPDFGenerator((this))))
        val updateDonationStatusEpic = createEpicMiddleware(UpdateDonationEpic(ioScheduler,
                mainScheduler,
                donationStore))

        Timber.d("creating store")
        store = createStore(reducers,
                initialState,
                applyMiddleware(appStateLoadingMiddleware,
                        donationLoadingMiddleWare,
                        obdSessionManagementMiddleware,
                        clearDTCsMiddleware,
                        fetchReportMiddleWare,
                        captureReportMiddleWare,
                        updateDonationStatusEpic))
        Timber.d("created store")


        persistenceStore.startListening(store)

        Notifier(this,
                store.asCustomObservable(),
                Schedulers.io(),
                AndroidSchedulers.mainThread(),
                Schedulers.computation())

        Timber.d("Initialization time ${System.currentTimeMillis() - initStartTime}")

    }


    private fun configureSeekRatingModule() {
        // Custom condition: 3 days and 5 launches
        val config = RateThisApp.Config(3, 5)
        RateThisApp.init(config)
    }

    //-------------------------------------------------AppContract-------------------------------------------------------------


    override fun onDataLoadingStartedFor(info: Vehicle) {
        //replace connection view with dashboard
        store.dispatch(CommonAppAction
                .ReplaceViewOnBackStackTop(DashboardScreen(DashboardScreenState
                        .ShowNewSnapshot(info,
                                LiveVehicleData(),
                                DashboardTheme.Dark))))
    }

    override fun onReportRequested() {
        store.dispatch(CommonAppAction.PushViewToBackStack(ReportScreen(ReportScreenState.ShowNewSnapshot(ReportData()))))
    }

    override fun killSession() {
        store.dispatch(BaseAppAction.KillActiveSession)
    }

    override fun enableReporting() {
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
            Fabric.with(this, Answers())
            reportingEnabled.set(true)
        }
    }

    override fun logEvent(event: AnswersEvent<*>) {
        if (!BuildConfig.DEBUG) {
            if (reportingEnabled.get()) {
                val answers = Answers.getInstance()
                when (event) {
                    is CustomEvent -> {
                        answers.logCustom(event)
                    }
                    is PurchaseEvent -> {
                        answers.logPurchase(event)
                    }
                    is AddToCartEvent -> {
                        answers.logAddToCart(event)
                    }
                    is ContentViewEvent -> {
                        answers.logContentView(event)
                    }
                    is LevelStartEvent -> {
                        answers.logLevelStart(event)
                    }
                    is LevelEndEvent -> {
                        answers.logLevelEnd(event)
                    }
                    is ShareEvent -> {
                        answers.logShare(event)
                    }

                }
            }
        }
    }

    override fun logNonFatal(error: Throwable) {
        if (!BuildConfig.DEBUG) {
            if (reportingEnabled.get()) {
                Crashlytics.logException(error)
            }
        }
    }

    override fun onPrivacyPolicyAccepted() {
        store.dispatch(BaseAppAction
                .UpdateAppSettings(store
                        .state
                        .getBaseAppState()
                        .baseAppPersistedState
                        .appSettings
                        .copy(privacyPolicyAccepted = true)))
    }
}