package com.exp.carconnect.dashboard.state

import com.exp.carconnect.base.CarConnectIndividualViewState
import com.exp.carconnect.base.CarConnectView

data class DashboardScreen(override val screenState: DashboardScreenState) : CarConnectView


sealed class DashboardScreenState : CarConnectIndividualViewState {
    object DisplayingLiveDashboard : DashboardScreenState()
}