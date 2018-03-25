package com.exp.carconnect.base.di

import com.exp.carconnect.base.viewmodel.SetupScreenVM
import com.exp.carconnect.obdlib.OBDEngine
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import io.reactivex.Scheduler
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Qualifier
import javax.inject.Scope

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Scope
annotation class NewOBDConnection


@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Io

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Computation

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Main


@Module()
class OBDConnectionModule(private val inputStream: InputStream,
                          private val outputStream: OutputStream) {

    @NewOBDConnection
    @Provides
    fun provideOBEngine(@Io ioScheduler: Scheduler,
                        @Computation computationScheduler: Scheduler) =
            OBDEngine(inputStream,
                    outputStream,
                    ioScheduler,
                    computationScheduler)
}

@NewOBDConnection
@Subcomponent(modules = [(OBDConnectionModule::class)])
interface NewOBDConnectionComponent {
    @Subcomponent.Builder
    interface Builder {
        fun requestModule(module: OBDConnectionModule): Builder
        fun build(): NewOBDConnectionComponent
    }

    fun inject(setupScreenVM: SetupScreenVM)

    @Main
    fun getMainScheduler(): Scheduler

    fun getOBDEngine(): OBDEngine
}