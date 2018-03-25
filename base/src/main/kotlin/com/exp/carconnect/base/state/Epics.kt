package com.exp.carconnect.base.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.store.loadAppState
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import redux.INIT
import redux.api.Store
import redux.observable.Epic

class BaseSateLoadingEpic(val ioScheduler: Scheduler,
                          val mainThreadScheduler: Scheduler) : Epic<AppState> {

    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {
        return actions
                .filter { it == INIT }
                .take(1)
                .flatMap {
                    Single.concat(
                            Single.just(BaseAppActions.LoadingBaseAppState),
                            loadAppState()
                                    .map<BaseAppActions> {
                                        //todo add appmode and apprunning mode in BaseAppState
                                        BaseAppActions.LoadedBaseAppState(BaseAppState(baseAppPersistedState = BaseAppPersistedState(it)))
                                    }
                                    .onErrorReturn {
                                        BaseAppActions.BaseAppStateLoadError(BaseAppStateLoadingError.UnkownError(it))
                                    }
                                    .subscribeOn(ioScheduler))
                            .observeOn(mainThreadScheduler)
                            .toObservable()
                }

    }


}
