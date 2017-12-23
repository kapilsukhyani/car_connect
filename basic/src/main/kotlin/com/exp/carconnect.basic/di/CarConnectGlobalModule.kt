package com.exp.carconnect.basic.di

import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Named
import javax.inject.Singleton

@Module(subcomponents = arrayOf(NewOBDConnectionComponent::class))
class CarConnectGlobalModule {


    @Singleton
    @Provides
    @Io
    fun provideIOScheduler(): Scheduler = Schedulers.io()

    @Singleton
    @Provides
    @Computation
    fun provideComputationScheduler(): Scheduler = Schedulers.computation()

    @Singleton
    @Provides
    @Main
    fun provideMainScheduler(): Scheduler = AndroidSchedulers.mainThread()

}