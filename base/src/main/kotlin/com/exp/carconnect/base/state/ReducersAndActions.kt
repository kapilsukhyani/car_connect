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
    object CloseSocketAndClearActiveSessionState : BaseAppAction()
    data class RefreshActiveSessionDataFetchRate(val settings: AppSettings) : BaseAppAction()

    data class UpdateAppSettings(val appSettings: AppSettings) : BaseAppAction()
    data class AddNewConnectedDongle(val dongle: Dongle) : BaseAppAction()
    data class AddNewVehicle(val vehicle: Vehicle) : BaseAppAction()

    data class AddEngineLoadToReport(val engineLoad: Float): BaseAppAction()
    data class AddFuelPressureToReport(val fuelPressure: Int): BaseAppAction()
    data class AddIntakeManifoldPressureToReport(val intakeMonitorPressure: Int): BaseAppAction()
    data class AddTimingAdvanceToReport(val timingAdvance: Float): BaseAppAction()
    data class AddMassAirFlowToReport(val massAirFlow: Float): BaseAppAction()
    data class AddRuntimeSinceEngineStartToReport(val runtimeSinceEngineStart: Int): BaseAppAction()
    data class AddDistanceTraveledSinceMILOnToReport(val distanceTraveledSinceMilOn: Int): BaseAppAction()
    data class AddFuelRailPressureToReport(val fuelRailPressure: Int): BaseAppAction()
    data class AddDistanceSinceDTCClearedToReport(val distanceSinceDTCCleared: Int): BaseAppAction()
    data class AddBarometricPressureToReport(val barometricPressure: Int): BaseAppAction()
    data class AddWideBandAirFuelRatioToReport(val wideBandAirFuelRatio: Float): BaseAppAction()
    data class AddModuleVoltageToReport(val moduleVoltage: Float): BaseAppAction()
    data class AddAbsoluteLoadToReport(val absoluteLoad: Float): BaseAppAction()
    data class AddFuelAirCommandedEquivalenceRatioToReport(val fuelAirCommandedEquivalenceRatio: Float): BaseAppAction()
    data class AddOilTemperatureToReport(val oilTemperature: Int): BaseAppAction()


    object ClearDTCs : BaseAppAction()
    object FetchReport : BaseAppAction()
    object UpdateClearDTCsOperationStateToClearing : BaseAppAction()
    object UpdateClearDTCsOperationStateToSuccessful : BaseAppAction()
    object UpdateClearDTCsOperationStateToNone : BaseAppAction()
    data class UpdateClearDTCsOperationStateToFailed(val error: ClearDTCError) : BaseAppAction()


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

            is BaseAppAction.UpdateAppSettings -> {
                state.copyAndReplaceAppSettings(action.appSettings)
            }

            is BaseAppAction.AddNewConnectedDongle -> {
                state.copyAndReplaceRecentlyUsedDongle(action.dongle)
            }

            is BaseAppAction.AddNewVehicle -> {
                state.copyAndReplaceRecentlyUsedDVehicle(action.vehicle)
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
            is BaseAppAction.CloseSocketAndClearActiveSessionState -> {
                if (state.isAnActiveSessionAvailable()) {
                    state.getActiveSession().socket.close()
                    state.clearActiveSessionState()
                } else {
                    state
                }
            }
            is BaseAppAction.AddVehicleDataLoadError -> {
                if (state.getBaseAppState().activeSession is UnAvailableAvailableData.Available<ActiveSession>) {
                    state.addActiveSession(state.getActiveSession().copy(liveVehicleData = LoadableState.LoadingError(action.error)))
                } else {
                    state
                }
            }

            else -> {
                if (state.isAnActiveSessionAvailable()) {
                    when (action) {

                        is BaseAppAction.AddAirIntakeTemperature -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(currentAirIntakeTemp = UnAvailableAvailableData.Available(action.temp))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddAmbientAirTemperature -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(currentAmbientTemp = UnAvailableAvailableData.Available(action.temp))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddRPM -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(rpm = UnAvailableAvailableData.Available(action.rpm))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }

                        is BaseAppAction.AddSpeed -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(speed = UnAvailableAvailableData.Available(action.speed))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddThrottlePosition -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(throttlePosition = UnAvailableAvailableData.Available(action.throttle))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddFuel -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(fuel = UnAvailableAvailableData.Available(action.fuel))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddMilStatus -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(milStatus = UnAvailableAvailableData.Available(action.milStatus))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddIgnition -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(ignition = UnAvailableAvailableData.Available(action.ignition))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }
                        is BaseAppAction.AddFuelConsumptionRate -> {
                            val vehicleData = getCurrentVehicleData(state)
                                    .copy(fuelConsumptionRate = UnAvailableAvailableData.Available(action.fuelConsumptionRate))
                            addNewLiveVehicleDataSnapShot(state, vehicleData)
                        }

                        is BaseAppAction.UpdateClearDTCsOperationStateToClearing -> {
                            state.addActiveSession(state.getActiveSession().copy(clearDTCsOperationState = ClearDTCOperationState.Clearing))
                        }
                        is BaseAppAction.UpdateClearDTCsOperationStateToSuccessful -> {
                            state.addActiveSession(state.getActiveSession().copy(clearDTCsOperationState = ClearDTCOperationState.Successful))
                        }
                        is BaseAppAction.UpdateClearDTCsOperationStateToFailed -> {
                            state.addActiveSession(state.getActiveSession().copy(clearDTCsOperationState = ClearDTCOperationState.Error(action.error)))
                        }
                        is BaseAppAction.UpdateClearDTCsOperationStateToNone -> {
                            state.addActiveSession(state.getActiveSession().copy(clearDTCsOperationState = ClearDTCOperationState.None))
                        }


                        is BaseAppAction.AddEngineLoadToReport -> {
                            val report = getCurrentReport(state).copy(engineLoad = UnAvailableAvailableData.Available(action.engineLoad))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddFuelPressureToReport -> {
                            val report = getCurrentReport(state).copy(fuelPressure = UnAvailableAvailableData.Available(action.fuelPressure))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddIntakeManifoldPressureToReport -> {
                            val report = getCurrentReport(state).copy(intakeManifoldPressure = UnAvailableAvailableData.Available(action.intakeMonitorPressure))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddTimingAdvanceToReport -> {
                            val report = getCurrentReport(state).copy(timingAdvance = UnAvailableAvailableData.Available(action.timingAdvance))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddMassAirFlowToReport -> {
                            val report = getCurrentReport(state).copy(massAirFlow = UnAvailableAvailableData.Available(action.massAirFlow))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddRuntimeSinceEngineStartToReport -> {
                            val report = getCurrentReport(state).copy(runTimeSinceEngineStart = UnAvailableAvailableData.Available(action.runtimeSinceEngineStart))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddDistanceTraveledSinceMILOnToReport -> {
                            val report = getCurrentReport(state).copy(distanceTravelledSinceMILOn = UnAvailableAvailableData.Available(action.distanceTraveledSinceMilOn))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddFuelRailPressureToReport -> {
                            val report = getCurrentReport(state).copy(fuelRailPressure = UnAvailableAvailableData.Available(action.fuelRailPressure))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddDistanceSinceDTCClearedToReport -> {
                            val report = getCurrentReport(state).copy(distanceSinceDTCCleared = UnAvailableAvailableData.Available(action.distanceSinceDTCCleared))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddBarometricPressureToReport -> {
                            val report = getCurrentReport(state).copy(barometricPressure = UnAvailableAvailableData.Available(action.barometricPressure))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddWideBandAirFuelRatioToReport -> {
                            val report = getCurrentReport(state).copy(wideBandAirFuelRatio = UnAvailableAvailableData.Available(action.wideBandAirFuelRatio))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddModuleVoltageToReport -> {
                            val report = getCurrentReport(state).copy(moduleVoltage = UnAvailableAvailableData.Available(action.moduleVoltage))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddAbsoluteLoadToReport -> {
                            val report = getCurrentReport(state).copy(absoluteLoad = UnAvailableAvailableData.Available(action.absoluteLoad))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddFuelAirCommandedEquivalenceRatioToReport -> {
                            val report = getCurrentReport(state).copy(fuelAirCommandedEquivalenceRatio = UnAvailableAvailableData.Available(action.fuelAirCommandedEquivalenceRatio))
                            addNewReportSnapShot(state, report)
                        }
                        is BaseAppAction.AddOilTemperatureToReport -> {
                            val report = getCurrentReport(state).copy(oilTemperature = UnAvailableAvailableData.Available(action.oilTemperature))
                            addNewReportSnapShot(state, report)
                        }

                        else -> {
                            state
                        }
                    }
                } else {
                    state
                }


            }
        }
    }

    private fun getCurrentVehicleData(state: AppState): LiveVehicleData {
        return if (state.getActiveSession().liveVehicleData !is LoadableState.Loaded) {
            LiveVehicleData()
        } else {
            (state.getActiveSession().liveVehicleData as LoadableState.Loaded).savedState
        }
    }


    private fun addNewLiveVehicleDataSnapShot(state: AppState, data: LiveVehicleData): AppState {
        return state.addActiveSession(state.getActiveSession().copy(liveVehicleData = LoadableState.Loaded(data)))

    }

    private fun getCurrentReport(state: AppState): Report {
        return if (state.getActiveSession().report !is LoadableState.Loaded) {
            Report()
        } else {
            (state.getActiveSession().report as LoadableState.Loaded).savedState
        }
    }


    private fun addNewReportSnapShot(state: AppState, data: Report): AppState {
        return state.addActiveSession(state.getActiveSession().copy(report = LoadableState.Loaded(data)))

    }


}


sealed class CommonAppAction {
    object BackPressed : CommonAppAction()
    object FinishCurrentView : CommonAppAction()
    data class PushViewToBackStack(val view: CarConnectView) : CommonAppAction()
    data class ReplaceViewOnBackStackTop(val view: CarConnectView) : CommonAppAction()
}

