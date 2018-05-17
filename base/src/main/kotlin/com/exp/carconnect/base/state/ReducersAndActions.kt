package com.exp.carconnect.base.state

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.base.*
import com.exp.carconnect.obdlib.OBDEngine
import redux.api.Reducer


sealed class BaseAppActions {
    object LoadingBaseAppState : BaseAppActions()
    data class LoadedBaseAppState(val state: BaseAppState) : BaseAppActions()
    data class BaseAppStateLoadError(val error: BaseAppStateLoadingError) : BaseAppActions()


    data class StartNewSession(val device: BluetoothDevice) : BaseAppActions()

    data class DeviceConnectionFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppActions()
    data class RunningSetup(val device: BluetoothDevice) : BaseAppActions()
    data class SetupCompleted(val device: BluetoothDevice) : BaseAppActions()
    data class SetupFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppActions()
    data class LoadingVehicleInfo(val device: BluetoothDevice) : BaseAppActions()
    data class VehicleInfoLoadingFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppActions()


    data class AddActiveSession(val device: BluetoothDevice, val socket: BluetoothSocket, val engine: OBDEngine) : BaseAppActions()
    data class AddVehicleInfoToActiveSession(val device: BluetoothDevice, val info: Vehicle) : BaseAppActions()
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


class AppStateNavigationReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            CommonAppAction.FinishCurrentView,
            CommonAppAction.BackPressed -> {
                state.popViewFromBackStack()
            }

            is CommonAppAction.PushViewToBackStack -> {
                state.pushViewToBackState(action.view)
            }
            is CommonAppAction.ReplaceViewOnBackStackTop -> {
                state.replaceViewAtStackTop(action.view)
            }

            else -> {
                state
            }
        }
    }

}

class ActiveSessionReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is BaseAppActions.AddActiveSession -> {
                state.addActiveSession(ActiveSession(Dongle(action.device.address, action.device.name),
                        action.socket, action.engine))
            }
            is BaseAppActions.AddVehicleInfoToActiveSession -> {
                state.addActiveSession(state.findActiveSession().copy(vehicle = LoadableState.Loaded(action.info)))
            }
            else -> {
                state
            }
        }
    }

}


sealed class CommonAppAction {
    object BackPressed : CommonAppAction()
    object FinishCurrentView : CommonAppAction()
    data class PushViewToBackStack(val view: CarConnectView) : CommonAppAction()
    data class ReplaceViewOnBackStackTop(val view: CarConnectView) : CommonAppAction()

}

