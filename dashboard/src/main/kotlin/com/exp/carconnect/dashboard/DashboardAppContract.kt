package com.exp.carconnect.dashboard

import com.exp.carconnect.dashboard.di.DashboardComponent

interface DashboardAppContract {
    fun buildNewDashboardComponent(): DashboardComponent
     var dashboardComponent: DashboardComponent?
}