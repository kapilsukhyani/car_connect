package com.exp.carconnect.base.store

import com.exp.carconnect.base.state.AppSettings
import com.exp.carconnect.base.state.Dongle
import com.exp.carconnect.base.state.Vehicle
import io.reactivex.Single

data class PersistedAppState(val knownDongles: Set<Dongle> = hashSetOf(),
                             val knownVehicles: Set<Vehicle> = hashSetOf(),
                             val lastConnectedDongle: Dongle? = null,
                             val lastConnectedVehicle: Vehicle? = null,
                             val appSettings: AppSettings = AppSettings())

internal fun loadAppState(): Single<PersistedAppState> {

    return Single.fromCallable {
        //todo implement this
        Thread.sleep(3000)
        PersistedAppState()
    }

}