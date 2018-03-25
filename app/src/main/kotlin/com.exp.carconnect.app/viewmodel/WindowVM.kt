package com.exp.carconnect.app.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.exp.carconnect.app.CarConnectApp
import com.exp.carconnect.app.state.NavigationActions
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.base.state.SplashScreenState
import io.reactivex.disposables.Disposable
import redux.api.Store


class WindowVM(app: Application) : AndroidViewModel(app) {


    companion object {
        const val TAG = "WindowVM"
    }

    private val currentViewLiveData: MutableLiveData<CarConnectView> = MutableLiveData()
    private val stateSubscription: Disposable
    private val store: Store<AppState> = getApplication<CarConnectApp>()
            .store

    init {
        store.dispatch(NavigationActions
                .ShowSplashScreen(SplashScreenState.LoadingAppState))
        stateSubscription = store
                .asCustomObservable()
                .map { it.uiState }
                .distinctUntilChanged()
                .subscribe({ uiState ->
                    currentViewLiveData.value = uiState.currentView
                }, { error ->
                    //todo, think what should be done if subscribing to store errors out
                })
    }


    override fun onCleared() {
        stateSubscription.dispose()
    }


    fun getCurrentViewLiveData(): LiveData<CarConnectView> {
        return currentViewLiveData
    }

    fun onBackPressed() {
        store.dispatch(CommonAppAction.BackPressed)
    }

}