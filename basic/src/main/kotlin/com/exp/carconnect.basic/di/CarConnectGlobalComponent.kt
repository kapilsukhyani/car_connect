package com.exp.carconnect.basic.di

import com.exp.carconnect.basic.CarConnectApp
import com.exp.carconnect.basic.viewmodel.OBDDashboardVM
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CarConnectGlobalModule::class))
interface CarConnectGlobalComponent {
    fun inject(app: CarConnectApp)
}
