package com.exp.carconnect.dashboard.di

import com.exp.carconnect.base.di.Main
import com.exp.carconnect.dashboard.viewmodel.OBDDashboardVM
import com.exp.carconnect.obdlib.OBDEngine
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import io.reactivex.Scheduler

@Module
class DashboardModule(val engine: OBDEngine) {
    @Provides
    fun provideOBDEngine(): OBDEngine {
        return engine
    }
}

@Subcomponent(modules = [DashboardModule::class])
interface DashboardComponent {

    @Subcomponent.Builder
    interface Builder {
        fun requestModule(module: DashboardModule): Builder
        fun build(): DashboardComponent
    }

    fun inject(obdDashboardVM: OBDDashboardVM)

    @Main
    fun getMainScheduler(): Scheduler

    fun getOBDEngine(): OBDEngine


}
