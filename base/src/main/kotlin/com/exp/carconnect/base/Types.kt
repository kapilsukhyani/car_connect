package com.exp.carconnect.base

import java.util.concurrent.TimeUnit

//----------------------------------- Generic data structures -------------------------------------------------

sealed class UnAvailableAvailableData<out T> {
    object UnAvailable : UnAvailableAvailableData<Nothing>()
    data class Available<out T : Any>(val data: T) : UnAvailableAvailableData<T>()
}

sealed class LoadableState<out State, out Error : Throwable> {
    object NotLoaded : LoadableState<Nothing, Nothing>()
    object Loading : LoadableState<Nothing, Nothing>()
    data class Loaded<out State>(val savedState: State) : LoadableState<State, Nothing>()
    data class LoadingError<out Error : Throwable>(val error: Error) : LoadableState<Nothing, Error>()
}

data class Frequency(val frequency: Long, val unit: TimeUnit)


//-------------------------------- State Aware View types -------------------------------------------------------


data class AppState(val moduleStateMap: Map<String, LoadableState<ModuleState, Throwable>>,
                    val uiState: CarConnectUIState)

interface ModuleState

data class CarConnectUIState(val backStack: List<CarConnectView> = emptyList()) {
    val currentView: CarConnectView? = if (backStack.isEmpty()) {
        null
    } else {
        backStack[backStack.size - 1]
    }
}

interface CarConnectIndividualViewState

interface CarConnectView {
    val screenState: CarConnectIndividualViewState
}


// ---------------------------------- Error management -----------------------------------------------------------

abstract class CarConnectError(val type: String) : Throwable(type) {
    //todo can this throwable be nullable?
    abstract val error: Throwable

    init {
        super.initCause(error)
    }
}


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