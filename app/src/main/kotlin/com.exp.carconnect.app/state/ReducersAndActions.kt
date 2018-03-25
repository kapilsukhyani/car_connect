package com.exp.carconnect.app.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.state.SplashScreen
import com.exp.carconnect.base.state.SplashScreenState
import redux.api.Reducer
import java.util.*

fun AppState.addViewToBackState(view: CarConnectView): AppState {

    val backStack = Stack<CarConnectView>()
    backStack.addAll(this.uiState.backStack + view)
    val uiState = this.uiState.copy(backStack = backStack,
            currentView = view)


    return this.copy(uiState = uiState)
}


sealed class NavigationActions {
    data class ShowSplashScreen(val state: SplashScreenState)
}

class AppStateNavigationReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            is NavigationActions.ShowSplashScreen -> {
                state.addViewToBackState(SplashScreen(action.state))
            }
            else -> {
                state
            }
        }
    }

}