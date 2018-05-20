package com.exp.carconnect.dashboard.state

import com.exp.carconnect.base.CarConnectIndividualViewState
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.base.state.VehicleData

data class DashboardScreen(override val screenState: DashboardScreenState) : CarConnectView


sealed class DashboardScreenState : CarConnectIndividualViewState {
    data class ShowNewSnapshot(val vehicle: Vehicle, val data: VehicleData) : DashboardScreenState()
    data class ShowError(val error: String) : DashboardScreenState()
}