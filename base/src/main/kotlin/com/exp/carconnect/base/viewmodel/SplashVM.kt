package com.exp.carconnect.base.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.base.state.SplashScreenState
import io.reactivex.disposables.CompositeDisposable


class SplashVM(app: Application) : AndroidViewModel(app) {

    private val splashScreenStateLiveData: MutableLiveData<SplashScreenState> = MutableLiveData()
    private val stateSubscription: CompositeDisposable = CompositeDisposable()

    private val store = (app as BaseAppContract).store

    init {
        stateSubscription.add(store
                .asCustomObservable()
                .map { it.moduleStateMap }
                .distinctUntilChanged()
                .map {
                    it.forEach { entry ->
                        when (entry.value) {
                            LoadableState.Loading,
                            LoadableState.NotLoaded -> {
                                return@map SplashScreenState.LoadingAppState
                            }
                            is LoadableState.LoadingError -> {
                                return@map SplashScreenState.ShowingLoadingError
                            }
                        }
                    }
                    store.dispatch(CommonAppAction.AppStateLoaded)
                    return@map SplashScreenState.LoadingAppState
                }
                .subscribe(
                        {
                            splashScreenStateLiveData.value = it

                        }, {
                    //todo log error
                }
                ))


    }

    fun getAppLoadingStateLiveData(): LiveData<SplashScreenState> {
        return splashScreenStateLiveData
    }

    override fun onCleared() {
        stateSubscription.dispose()
    }


}