package com.exp.carconnect.dashboard.state

sealed class DashboardAction {
    data class AddNewDashboardState(val state: DashboardScreenState) : DashboardAction()
}