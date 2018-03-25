package com.exp.carconnect.base.di

import io.reactivex.Scheduler
import javax.inject.Singleton


interface Dependencies {

    @Singleton
    @Io
    fun provideIOScheduler(): Scheduler

    @Singleton
    @Computation
    fun provideComputationScheduler(): Scheduler

    @Singleton
    @Main
    fun provideMainScheduler(): Scheduler
}