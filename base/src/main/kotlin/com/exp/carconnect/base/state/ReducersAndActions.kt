package com.exp.carconnect.base.state

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.base.*
import com.exp.carconnect.obdlib.OBDEngine
import redux.api.Reducer


sealed class BaseAppAction {
    object LoadingBaseAppState : BaseAppAction()
    data class LoadedBaseAppState(val state: BaseAppState) : BaseAppAction()
    data class BaseAppStateLoadError(val error: BaseAppStateLoadingError) : BaseAppAction()


    data class StartNewSession(val device: BluetoothDevice) : BaseAppAction()

    data class DeviceConnectionFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppAction()
    data class RunningSetup(val device: BluetoothDevice) : BaseAppAction()
    data class SetupCompleted(val device: BluetoothDevice) : BaseAppAction()
    data class SetupFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppAction()
    data class LoadingVehicleInfo(val device: BluetoothDevice) : BaseAppAction()
    data class VehicleInfoLoadingFailed(val device: BluetoothDevice, val error: Throwable) : BaseAppAction()


    data class AddActiveSession(val device: BluetoothDevice, val socket: BluetoothSocket, val engine: OBDEngine) : BaseAppAction()
    data class AddVehicleInfoToActiveSession(val device: BluetoothDevice, val info: Vehicle) : BaseAppAction()
    data class AddAirIntakeTemperature(val temp: Float) : BaseAppAction()
    data class AddAmbientAirTemperature(val temp: Float) : BaseAppAction()
    data class AddRPM(val rpm: Float) : BaseAppAction()
    data class AddSpeed(val speed: Float) : BaseAppAction()
    data class AddThrottlePosition(val throttle: Float) : BaseAppAction()
    data class AddFuel(val fuel: Float) : BaseAppAction()
    data class AddIgnition(val ignition: Boolean) : BaseAppAction()
    data class AddMilStatus(val milStatus: MILStatus) : BaseAppAction()
    data class AddFuelConsumptionRate(val fuelConsumptionRate: Float) : BaseAppAction()
    data class AddVehicleDataLoadError(val error: Throwable) : BaseAppAction()
    object KillActiveSession : BaseAppAction()


}


class BaseAppStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            BaseAppAction.LoadingBaseAppState -> {
                state.copyAndReplaceBaseAppState(LoadableState.Loading)
            }
            is BaseAppAction.LoadedBaseAppState -> {
                state.copyAndReplaceBaseAppState(LoadableState.Loaded(action.state))
            }

            is BaseAppAction.BaseAppStateLoadError -> {
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
            is BaseAppAction.AddActiveSession -> {
                state.addActiveSession(ActiveSession(Dongle(action.device.address, action.device.name),
                        action.socket, action.engine))
            }
            is BaseAppAction.AddVehicleInfoToActiveSession -> {
                state.addActiveSession(state.getActiveSession().copy(vehicle = LoadableState.Loaded(action.info)))
            }
            is BaseAppAction.KillActiveSession -> {
                val session = state.getBaseAppState().activeSession
                if (session is UnAvailableAvailableData.Available<ActiveSession>) {
                    session.data.socket.close()
                    state.killActiveSession()
                } else {
                    state
                }
            }
            is BaseAppAction.AddAirIntakeTemperature -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(currentAirIntakeTemp = UnAvailableAvailableData.Available(action.temp))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddAmbientAirTemperature -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(currentAmbientTemp = UnAvailableAvailableData.Available(action.temp))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddRPM -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(rpm = UnAvailableAvailableData.Available(action.rpm))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddSpeed -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(speed = UnAvailableAvailableData.Available(action.speed))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddThrottlePosition -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(throttlePosition = UnAvailableAvailableData.Available(action.throttle))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddFuel -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(fuel = UnAvailableAvailableData.Available(action.fuel))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddMilStatus -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(milStatus = UnAvailableAvailableData.Available(action.milStatus))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddIgnition -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(ignition = UnAvailableAvailableData.Available(action.ignition))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddFuelConsumptionRate -> {
                val vehicleData = getCurrentVehicleData(state)
                        .copy(fuelConsumptionRate = UnAvailableAvailableData.Available(action.fuelConsumptionRate))
                addNewSnapShot(state, vehicleData)
            }
            is BaseAppAction.AddVehicleDataLoadError -> {
                state.addActiveSession(state.getActiveSession().copy(currentVehicleData = LoadableState.LoadingError(action.error)))
            }

            else -> {
                state
            }
        }
    }

    private fun getCurrentVehicleData(state: AppState): VehicleData {
        return if (state.getActiveSession().currentVehicleData !is LoadableState.Loaded) {
            VehicleData()
        } else {
            (state.getActiveSession().currentVehicleData as LoadableState.Loaded).savedState
        }
    }

    private fun addNewSnapShot(state: AppState, data: VehicleData): AppState {
        return state.addActiveSession(state.getActiveSession().copy(currentVehicleData = LoadableState.Loaded(data)))

    }

}


sealed class CommonAppAction {
    object BackPressed : CommonAppAction()
    object FinishCurrentView : CommonAppAction()
    data class PushViewToBackStack(val view: CarConnectView) : CommonAppAction()
    data class ReplaceViewOnBackStackTop(val view: CarConnectView) : CommonAppAction()
    object ShowDataView : CommonAppAction()

}

