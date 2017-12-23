package com.exp.carconnect.basic.di

import com.exp.carconnect.basic.viewmodel.OBDDashboardVM
import com.exp.carconnect.basic.viewmodel.SetupScreenVM
import dagger.Subcomponent

@OBDConnection
@Subcomponent(modules = arrayOf(OBDConnectionModule::class))
interface NewOBDConnectionComponent {
    @Subcomponent.Builder
    interface Builder {
        fun requestModule(module: OBDConnectionModule): Builder
        fun build(): NewOBDConnectionComponent
    }

    fun inject(dashboardVM: OBDDashboardVM)
    fun inject(setupScreenVM: SetupScreenVM)
}