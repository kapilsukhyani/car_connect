package com.exp.carconnect.base.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.OBDDeviceSessionManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import redux.INIT
import redux.api.Store
import redux.observable.Epic

class BaseSateLoadingEpic(private val ioScheduler: Scheduler,
                          private val mainThreadScheduler: Scheduler,
                          private val baseAppContract: BaseAppContract) : Epic<AppState> {

    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {
        return actions
                .filter { it == INIT }
                .take(1)
                .flatMap {
                    Single.concat(
                            Single.just(BaseAppAction.LoadingBaseAppState),
                            baseAppContract
                                    .persistenceStore
                                    .loadAppState()
                                    .map<BaseAppAction> {
                                        //todo add appmode and apprunning mode in BaseAppState
                                        BaseAppAction.LoadedBaseAppState(BaseAppState(baseAppPersistedState = BaseAppPersistedState(it)))
                                    }
                                    .onErrorReturn {
                                        BaseAppAction.BaseAppStateLoadError(BaseAppStateLoadingError.UnkownError(it))
                                    }
                                    .subscribeOn(ioScheduler))
                            .observeOn(mainThreadScheduler)
                            .toObservable()
                }

    }


}

class OBDSessionManagementEpic(private val ioScheduler: Scheduler,
                               private val mainThreadScheduler: Scheduler,
                               private val sessionManager: OBDDeviceSessionManager) : Epic<AppState> {
    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {

        return actions
                .filter {
                    it is BaseAppAction.StartNewSession ||
                            it === BaseAppAction.KillActiveSession ||
                            it is BaseAppAction.RefreshActiveSessionDataFetchRate
                }

                .switchMap {
                    when (it) {
                        is BaseAppAction.StartNewSession -> {
                            sessionManager.startNewSession(it.device,
                                    store.state.getAppSettings())
                                    .subscribeOn(ioScheduler)
                                    .observeOn(mainThreadScheduler)
                        }
                        BaseAppAction.KillActiveSession -> {
                            sessionManager.killActiveSession()
                            Observable.just(BaseAppAction.CloseSocketAndClearActiveSessionState)
                        }
                        is BaseAppAction.RefreshActiveSessionDataFetchRate -> {
                            if (!store.state.isAnActiveSessionAvailable()) {
                                Observable.empty()
                            } else {
                                val activeSession = store.state.getActiveSession()
                                sessionManager
                                        .updateDataLoadFrequencyForActiveSession(activeSession.engine, it.settings)
                                        ?.subscribeOn(ioScheduler)
                                        ?.observeOn(mainThreadScheduler)
                                        ?: Observable.empty()
                            }
                        }
                        else -> {
                            Observable.just(it)
                        }
                    }

                }
    }

}
