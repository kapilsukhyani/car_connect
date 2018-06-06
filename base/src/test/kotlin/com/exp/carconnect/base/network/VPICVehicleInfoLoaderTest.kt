package com.exp.carconnect.base.network

import org.junit.Assert.*
import org.junit.Test

class VPICVehicleInfoLoaderTest{

    @Test
    fun testLoad() {
        val loaded = VPICVehicleInfoLoader()
        loaded.loadVehicleInfo("1GNEC13K3SJ445255").subscribe { it ->
            println(it)
        }
    }
}