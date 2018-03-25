package com.exp.carconnect.base.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.LoadableState
import redux.api.Reducer


sealed class BaseAppActions {
    object LoadingBaseAppState : BaseAppActions()
    data class LoadedBaseAppState(val state: BaseAppState) : BaseAppActions()
    data class BaseAppStateLoadError(val error: BaseAppStateLoadingError) : BaseAppActions()

}


class BaseAppStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            BaseAppActions.LoadingBaseAppState -> {
                state.copyAndReplaceBaseAppState(LoadableState.Loading)
            }
            is BaseAppActions.LoadedBaseAppState -> {
                state.copyAndReplaceBaseAppState(LoadableState.Loaded(action.state))
            }

            is BaseAppActions.BaseAppStateLoadError -> {
                state.copyAndReplaceBaseAppState(LoadableState.LoadingError(action.error))
            }
            else -> {
                state
            }
        }

    }

}

