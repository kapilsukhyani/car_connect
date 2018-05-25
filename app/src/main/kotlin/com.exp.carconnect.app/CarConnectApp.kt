package com.exp.carconnect.app

import android.app.Application
import com.exp.carconnect.base.*
import com.exp.carconnect.base.di.CarConnectGlobalComponent
import com.exp.carconnect.base.di.DaggerCarConnectGlobalComponent
import com.exp.carconnect.base.di.NewOBDConnectionComponent
import com.exp.carconnect.base.di.OBDConnectionModule
import com.exp.carconnect.base.fragment.DeviceConnectionScreenStateReducer
import com.exp.carconnect.base.fragment.DeviceManagementScreenStateReducer
import com.exp.carconnect.base.state.*
import com.exp.carconnect.base.store.BaseStore
import com.exp.carconnect.dashboard.DashboardAppContract
import com.exp.carconnect.dashboard.di.DashboardComponent
import com.exp.carconnect.dashboard.di.DashboardModule
import com.exp.carconnect.dashboard.state.DashboardScreen
import com.exp.carconnect.dashboard.state.DashboardScreenState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import redux.api.Store
import redux.applyMiddleware
import redux.combineReducers
import redux.createStore
import redux.observable.createEpicMiddleware
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject


class CarConnectApp : Application(),
        BaseAppContract,
        DashboardAppContract {


    companion object {
        const val TAG = "CarConnectApp"
    }

    lateinit var globalComponent: CarConnectGlobalComponent
    override lateinit var store: Store<AppState>
    override lateinit var persistenceStore: BaseStore

    @Inject
    lateinit var newConnectionComponentBuilder: NewOBDConnectionComponent.Builder
    @Inject
    lateinit var dashboardComponentBuilder: DashboardComponent.Builder


    override var newOBDConnectionComponent: NewOBDConnectionComponent? = null
    override var dashboardComponent: DashboardComponent? = null


    override fun onCreate() {
        super.onCreate()
        globalComponent = DaggerCarConnectGlobalComponent
                .builder()
                .build()
        globalComponent.inject(this)
        //        Fabric.with(this, Crashlytics())

        persistenceStore = BaseStore(this, Schedulers.io())

        val reducers = combineReducers(AppStateNavigationReducer(),
                BaseAppStateReducer(), DeviceManagementScreenStateReducer(), DeviceConnectionScreenStateReducer(this), ActiveSessionReducer())
        val initialState = AppState(mapOf(Pair(BaseAppState.STATE_KEY, LoadableState.NotLoaded)),
                CarConnectUIState(Stack()))
        val appStateLoadingMiddleware = createEpicMiddleware(BaseSateLoadingEpic(Schedulers.io(), AndroidSchedulers.mainThread(), this))
        val obdSessionManagementMiddleware = createEpicMiddleware(OBDSessionManagementEpic(Schedulers.io(), AndroidSchedulers.mainThread()
                , OBDDeviceSessionManager(Schedulers.io(), Schedulers.computation(), AndroidSchedulers.mainThread())))

        println("debugtag: creating store")

        store = createStore(reducers, initialState, applyMiddleware(appStateLoadingMiddleware, obdSessionManagementMiddleware))
        store
                .asCustomObservable()
                .filter { it.isBaseStateLoaded() }
                .map { it.getBaseAppState().activeSession }
                .filter {
                    it is UnAvailableAvailableData.Available<ActiveSession>
                            && it.data.vehicle is LoadableState.Loaded
                }
                .take(1)
                .map {
                    val activeSession = (it as UnAvailableAvailableData.Available).data
                    Pair((activeSession.vehicle as LoadableState.Loaded).savedState, VehicleData())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    store.dispatch(CommonAppAction.PushViewToBackStack(DashboardScreen(DashboardScreenState.ShowNewSnapshot(it.first, it.second))))
                }
        println("debugtag: created store")
        persistenceStore.startListening(store)

    }


    //-------------------------------------------------AppContract-------------------------------------------------------------

    override fun buildNewOBDConnectionComponent(inputStream: InputStream, outputStream: OutputStream): NewOBDConnectionComponent {
        newOBDConnectionComponent = newConnectionComponentBuilder
                .requestModule(OBDConnectionModule(inputStream, outputStream))
                .build()

        return newOBDConnectionComponent!!
    }


    override fun buildNewDashboardComponent(): DashboardComponent {
        dashboardComponent = dashboardComponentBuilder
                .requestModule(DashboardModule(newOBDConnectionComponent!!
                        .getOBDEngine()))
                .build()
        return dashboardComponent!!
    }


}