package com.exp.carconnect.base.state

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.LoadableState
import redux.api.Reducer


sealed class BaseAppActions {
    object LoadingBaseAppState : BaseAppActions()
    data class LoadedBaseAppState(val state: BaseAppState) : BaseAppActions()
    data class BaseAppStateLoadError(val error: BaseAppStateLoadingError) : BaseAppActions()

    data class StartNewSession(val device: BluetoothDevice) : BaseAppActions()
    data class SessionStarted(val device: BluetoothDevice) : BaseAppActions()
    data class DeviceConnected(val device: BluetoothDevice, val socket: BluetoothSocket) : BaseAppActions()
    data class DeviceConnectionFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppActions()
    data class RunningSetup(val device: BluetoothDevice) : BaseAppActions()
    data class SetupCompleted(val device: BluetoothDevice) : BaseAppActions()
    data class SetupFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppActions()
    data class LoadingVehicleInfo(val device: BluetoothDevice) : BaseAppActions()
    data class VehicleInfoLoadingFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppActions()
    data class VehicleInfoLoaded(val device: BluetoothDevice, val info: Vehicle) : BaseAppActions()

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


sealed class CommonAppAction {
    object AppStateLoaded : CommonAppAction()
    object BackPressed : CommonAppAction()
    object FinishView : CommonAppAction()
}

