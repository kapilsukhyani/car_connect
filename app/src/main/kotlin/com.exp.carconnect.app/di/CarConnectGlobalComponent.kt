package com.exp.carconnect.base.di

import com.exp.carconnect.app.CarConnectApp
import com.exp.carconnect.dashboard.di.DashboardComponent
import dagger.Component
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton


@Module(subcomponents = [(NewOBDConnectionComponent::class),
    (DashboardComponent::class)])
class CarConnectGlobalModule : Dependencies {

    @Singleton
    @Provides
    @Io
    override fun provideIOScheduler(): Scheduler = Schedulers.io()

    @Singleton
    @Provides
    @Computation
    override fun provideComputationScheduler(): Scheduler = Schedulers.computation()

    @Singleton
    @Provides
    @Main
    override fun provideMainScheduler(): Scheduler = AndroidSchedulers.mainThread()

}


@Singleton
@Component(modules = [(CarConnectGlobalModule::class)])
interface CarConnectGlobalComponent {
    fun inject(app: CarConnectApp)
}
