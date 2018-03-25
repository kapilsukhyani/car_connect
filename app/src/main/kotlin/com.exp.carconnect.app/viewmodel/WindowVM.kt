package com.exp.carconnect.app.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.exp.carconnect.app.CarConnectApp
import com.exp.carconnect.app.state.NavigationActions
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.SplashScreenState
import io.reactivex.disposables.Disposable


class WindowVM(app: Application) : AndroidViewModel(app) {


    companion object {
        const val TAG = "WindowVM"
    }

    private val currentViewLiveData: MutableLiveData<CarConnectView> = MutableLiveData()

    private val stateSubscription: Disposable

    init {

        stateSubscription = getApplication<CarConnectApp>()
                .store
                .asCustomObservable()
                .map { it.uiState }
                .distinctUntilChanged()
                .subscribe({ uiState ->
                    if (uiState.currentView == null) {
                        //cold boot
                        getApplication<CarConnectApp>()
                                .store
                                .dispatch(NavigationActions
                                        .ShowSplashScreen(SplashScreenState.LoadingAppState))
                    } else {
                        currentViewLiveData.value = uiState.currentView
                    }

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

}