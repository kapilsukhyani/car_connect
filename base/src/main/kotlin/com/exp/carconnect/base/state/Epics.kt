package com.exp.carconnect.base.state

import io.reactivex.Observable
import redux.api.Store
import redux.observable.Epic

class SateLoadingEpic : Epic<AppState> {

   
    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {
        return actions
//       actions.ofType(CarConnectHighLevelActions.InitApp::class.java).flatMap {  }
    }

}
