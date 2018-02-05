package com.exp.carconnect.base.di

import com.exp.carconnect.base.viewmodel.SetupScreenVM
import dagger.Subcomponent

@OBDConnection
@Subcomponent(modules = arrayOf(OBDConnectionModule::class))
interface NewOBDConnectionComponent {
    @Subcomponent.Builder
    interface Builder {
        fun requestModule(module: OBDConnectionModule): Builder
        fun build(): NewOBDConnectionComponent
    }

    fun inject(injectable: Injectable)
    fun inject(setupScreenVM: SetupScreenVM)
}