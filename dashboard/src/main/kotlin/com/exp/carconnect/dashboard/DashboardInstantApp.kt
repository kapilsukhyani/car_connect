package com.exp.carconnect.dashboard

import com.exp.carconnect.base.CarConnectAbstractApp


class DashboardInstantApp : CarConnectAbstractApp() {
    override fun getMode(): String {
        return "Instant App- Dashboard"
    }
}