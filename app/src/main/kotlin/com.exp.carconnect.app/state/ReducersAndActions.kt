package com.exp.carconnect.app.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.state.*
import redux.api.Reducer

fun AppState.pushViewToBackState(view: CarConnectView): AppState {
    val uiState = this.uiState.copy(backStack = this.uiState.backStack + view)
    return this.copy(uiState = uiState)
}

fun AppState.popViewFromBackStack(): AppState {
    val uiState = this.uiState.copy(backStack = this.uiState.backStack.subList(0, this.uiState.backStack.size - 1))
    return this.copy(uiState = uiState)
}

fun AppState.replaceViewAtStackTop(view: CarConnectView): AppState {
    val uiState = this.uiState.copy(backStack = this.uiState.backStack.subList(0, this.uiState.backStack.size - 1) + view)
    return this.copy(uiState = uiState)
}


sealed class NavigationActions {
    data class ShowSplashScreen(val state: SplashScreenState): NavigationActions()
}

class AppStateNavigationReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            is NavigationActions.ShowSplashScreen -> {
                state.pushViewToBackState(SplashScreen(action.state))
            }

            CommonAppAction.FinishView -> {
                state.popViewFromBackStack()
            }

            CommonAppAction.AppStateLoaded -> {
                state.replaceViewAtStackTop(DeviceManagementScreen(DeviceManagementScreenState.LoadingDevices))
            }
            CommonAppAction.BackPressed -> {
                state.popViewFromBackStack()
            }
            is BaseAppActions.SessionStarted -> {
                state.pushViewToBackState(ConnectionScreen(ConnectionScreenState.Connecting(action.device)))
            }
            else -> {
                state
            }
        }
    }

}