package com.exp.carconnect.base.di

import com.exp.carconnect.base.CarConnectAbstractApp
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CarConnectGlobalModule::class))
interface CarConnectGlobalComponent {
    fun inject(app: CarConnectAbstractApp)
}
