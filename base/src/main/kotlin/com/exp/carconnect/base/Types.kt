package com.exp.carconnect.base

import com.exp.carconnect.base.state.SplashScreen
import com.exp.carconnect.base.state.SplashScreenState
import java.util.*
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

data class CarConnectUIState(val backStack: Stack<out CarConnectView> = defaultBackStack(),
                             val currentView: CarConnectView? = try {
                                 backStack.peek()
                             } catch (ex: EmptyStackException) {
                                 null
                             })

interface CarConnectIndividualViewState

interface CarConnectView {
    val screenState: CarConnectIndividualViewState
}

fun defaultBackStack(): Stack<CarConnectView> {
    return Stack<CarConnectView>().apply { push(SplashScreen(SplashScreenState.LoadingAppState)) }
}


// ---------------------------------- Error management -----------------------------------------------------------

abstract class CarConnectError(val type: String) : Throwable(type) {
    //todo can this throwable be nullable?
    abstract val error: Throwable

    init {
        super.initCause(error)
    }
}