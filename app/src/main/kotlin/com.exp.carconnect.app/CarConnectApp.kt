package com.exp.carconnect.app

import android.app.Application
import com.exp.carconnect.app.state.AppStateNavigationReducer
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.CarConnectUIState
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.di.CarConnectGlobalComponent
import com.exp.carconnect.base.di.DaggerCarConnectGlobalComponent
import com.exp.carconnect.base.di.NewOBDConnectionComponent
import com.exp.carconnect.base.di.OBDConnectionModule
import com.exp.carconnect.base.fragment.DeviceManagementScreenStateReducer
import com.exp.carconnect.base.state.BaseAppState
import com.exp.carconnect.base.state.BaseAppStateReducer
import com.exp.carconnect.base.state.BaseSateLoadingEpic
import com.exp.carconnect.dashboard.DashboardAppContract
import com.exp.carconnect.dashboard.di.DashboardComponent
import com.exp.carconnect.dashboard.di.DashboardModule
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

        val reducers = combineReducers(AppStateNavigationReducer(),
                BaseAppStateReducer(), DeviceManagementScreenStateReducer())
        val initialState = AppState(mapOf(Pair(BaseAppState.STATE_KEY, LoadableState.NotLoaded)),
                CarConnectUIState(Stack()))
        val middleware = createEpicMiddleware(BaseSateLoadingEpic(Schedulers.io(), AndroidSchedulers.mainThread()))

        println("debugtag: creating store")

        store = createStore(reducers, initialState, applyMiddleware(middleware))

        println("debugtag: created store")

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