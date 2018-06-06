package com.exp.carconnect.base.network

import io.reactivex.Single

data class VehicleInfo(val make: String,
                       val model: String,
                       val manufacturer: String,
                       val modelYear: String)

interface VehicleInfoLoader {
    fun loadVehicleInfo(vin: String): Single<VehicleInfo>
}

interface VehicleInfoLoaderFactory {
    fun getVehicleInfoLoader(): VehicleInfoLoader
}

class VehicleInfoLoaderFactoryImpl : VehicleInfoLoaderFactory {
    override fun getVehicleInfoLoader(): VehicleInfoLoader {
        return VPICVehicleInfoLoader()
    }
}