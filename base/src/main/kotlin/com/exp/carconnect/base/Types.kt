package com.exp.carconnect.base

import java.util.*
import java.util.concurrent.TimeUnit

//----------------------------------- Generic data structures -------------------------------------------------

sealed class UnAvailableAvailableData<out T> {
    object UnAvailable : UnAvailableAvailableData<Nothing>() {
        override fun toString(): String {
            return "N/A"
        }
    }

    data class Available<out T : Any>(val data: T) : UnAvailableAvailableData<T>() {
        override fun toString(): String {
            return data.toString()
        }
    }
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


sealed class BackStackState {
    data class PushingViews(val carConnectViews: Array<CarConnectView>) : BackStackState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PushingViews

            if (!Arrays.equals(carConnectViews, other.carConnectViews)) return false

            return true
        }

        override fun hashCode(): Int {
            return Arrays.hashCode(carConnectViews)
        }
    }

    data class PoppingViews(val carConnectViews: Array<CarConnectView>) : BackStackState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PoppingViews

            if (!Arrays.equals(carConnectViews, other.carConnectViews)) return false

            return true
        }

        override fun hashCode(): Int {
            return Arrays.hashCode(carConnectViews)
        }
    }

    data class ReplacingViews(val oldViews: Array<CarConnectView>, val newViews: Array<CarConnectView>) : BackStackState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ReplacingViews

            if (!Arrays.equals(oldViews, other.oldViews)) return false
            if (!Arrays.equals(newViews, other.newViews)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = Arrays.hashCode(oldViews)
            result = 31 * result + Arrays.hashCode(newViews)
            return result
        }

    }

}

data class CarConnectUIState(val backStack: List<CarConnectView> = emptyList(),
                             val result: ViewResult? = null,
                             val backStackState: BackStackState? = null) {
    val currentView: CarConnectView? = if (backStack.isEmpty()) {
        null
    } else {
        backStack[backStack.size - 1]
    }

    fun isRestoringCurrentViewFromBackStack(): Boolean {
        return backStackState != null && backStackState!! is BackStackState.PoppingViews
    }
}

interface ViewResult
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
    val uiState = this.uiState.copy(backStack = this.uiState.backStack + view,
            backStackState = BackStackState.PushingViews(arrayOf(view)))
    return this.copy(uiState = uiState)
}

fun AppState.popViewFromBackStack(): AppState {
    val uiState = this.uiState.copy(backStack = this.uiState.backStack.subList(0, this.uiState.backStack.size - 1),
            backStackState = BackStackState.PoppingViews(arrayOf(this.uiState.backStack[this.uiState.backStack.size - 1])))
    return this.copy(uiState = uiState)
}

fun AppState.replaceViewAtStackTop(view: CarConnectView): AppState {
    val uiState = this.uiState.copy(backStack = this.uiState.backStack.subList(0, this.uiState.backStack.size - 1) + view,
            backStackState = BackStackState.ReplacingViews(arrayOf(this.uiState.backStack[this.uiState.backStack.size - 1]), arrayOf(view)))
    return this.copy(uiState = uiState)
}