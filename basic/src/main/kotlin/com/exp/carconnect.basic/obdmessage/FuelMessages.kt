package com.exp.carconnect.basic.obdmessage

import java.util.*

class AirFuelRatioRequest(retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("AirFuelRatioRequest", "01 44", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return TimingAdvanceResponse(rawResponse)
    }
}

class AirFuelRatioResponse(rawResponse: String) : OBDResponse("AirFuelRatioResponse", rawResponse) {
    val afr: Float

    init {
        val buffer = rawResponse.toIntList()
        // ignore first two bytes [01 44] of the response
        afr = (buffer[2] * 256 + buffer[3]) / 32768 * 14.7f//((A*256)+B)/32768
    }

    override fun getFormattedResult(): String {
        return "$afr:1 AFR"
    }
}


class ConsumptionRateRequest(retriable: Boolean = true,
                             repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("ConsumptionRateRequest", "01 5E", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return ConsumptionRateResponse(rawResponse)
    }
}

class ConsumptionRateResponse(rawResponse: String) : OBDResponse("ConsumptionRateResponse", rawResponse) {
    val fuelRate: Float

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        fuelRate = (buffer[2] * 256 + buffer[3]) * 0.05f
    }

    override fun getFormattedResult(): String {
        return "$fuelRate L/h"
    }
}


class FuelTypeRequest(retriable: Boolean = true) :
        OBDRequest("FuelTypeRequest", "01 51", retriable, IsRepeatable.No, true) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return FuelTypeResponse(rawResponse)
    }
}

class FuelTypeResponse(rawResponse: String) : OBDResponse("FuelTypeResponse", rawResponse) {
    val fuelType: Int

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        fuelType = buffer[2]
    }

    override fun getFormattedResult(): String {
        return try {
            FuelType.fromValue(fuelType).description
        } catch (e: NullPointerException) {
            "-"
        }

    }
}


enum class FuelType (val value: Int,
                                        val description: String) {

    GASOLINE(0x01, "Gasoline"),
    METHANOL(0x02, "Methanol"),
    ETHANOL(0x03, "Ethanol"),
    DIESEL(0x04, "Diesel"),
    LPG(0x05, "GPL/LGP"),
    CNG(0x06, "Natural Gas"),
    PROPANE(0x07, "Propane"),
    ELECTRIC(0x08, "Electric"),
    BIFUEL_GASOLINE(0x09, "Biodiesel + Gasoline"),
    BIFUEL_METHANOL(0x0A, "Biodiesel + Methanol"),
    BIFUEL_ETHANOL(0x0B, "Biodiesel + Ethanol"),
    BIFUEL_LPG(0x0C, "Biodiesel + GPL/LGP"),
    BIFUEL_CNG(0x0D, "Biodiesel + Natural Gas"),
    BIFUEL_PROPANE(0x0E, "Biodiesel + Propane"),
    BIFUEL_ELECTRIC(0x0F, "Biodiesel + Electric"),
    BIFUEL_GASOLINE_ELECTRIC(0x10, "Biodiesel + Gasoline/Electric"),
    HYBRID_GASOLINE(0x11, "Hybrid Gasoline"),
    HYBRID_ETHANOL(0x12, "Hybrid Ethanol"),
    HYBRID_DIESEL(0x13, "Hybrid Diesel"),
    HYBRID_ELECTRIC(0x14, "Hybrid Electric"),
    HYBRID_MIXED(0x15, "Hybrid Mixed"),
    HYBRID_REGENERATIVE(0x16, "Hybrid Regenerative");


    companion object {
        private val map = HashMap<Int, FuelType>()

        init {
            for (error in FuelType.values())
                map.put(error.value, error)
        }

        fun fromValue(value: Int): FuelType {
            return map[value]!!
        }
    }

}

class FuelLevelRequest(retriable: Boolean = true,
                       repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("FuelLevelRequest", "01 2F", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return FuelLevelResponse(rawResponse)
    }
}

class FuelLevelResponse(rawResponse: String) : OBDResponse("FuelLevelResponse", rawResponse) {
    val fuelLevel: Float

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        fuelLevel = 100.0f * buffer[2] / 255.0f
    }

    override fun getFormattedResult(): String {
        return "$fuelLevel %"
    }
}

class FuelTrimRequest(fuelTrim: FuelTrim, retriable: Boolean = true,
                      repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("FuelTrimRequest[${fuelTrim.bank}]", fuelTrim.buildObdCommand(), retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return FuelTrimResponse(rawResponse)
    }
}

class FuelTrimResponse(rawResponse: String) : OBDResponse("FuelTrimResponse", rawResponse) {
    val fuelTrim: Float

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        fuelTrim = (buffer[2] - 128) * (100.0f / 128)
    }

    override fun getFormattedResult(): String {
        return "$fuelTrim %"
    }
}

enum class FuelTrim (val value: Int,
                                        val bank: String) {

    SHORT_TERM_BANK_1(0x06, "Short Term Fuel Trim Bank 1"),
    LONG_TERM_BANK_1(0x07, "Long Term Fuel Trim Bank 1"),
    SHORT_TERM_BANK_2(0x08, "Short Term Fuel Trim Bank 2"),
    LONG_TERM_BANK_2(0x09, "Long Term Fuel Trim Bank 2");

    fun buildObdCommand(): String {
        return "01 0" + value
    }

    companion object {

        private val map = HashMap<Int, FuelTrim>()

        init {
            for (error in FuelTrim.values())
                map.put(error.value, error)
        }

        fun fromValue(value: Int): FuelTrim {
            return map[value]!!
        }
    }

}

class WidebandAirFuelRatioRequest(retriable: Boolean = true,
                                  repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("WidebandAirFuelRatioRequest", "01 34", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return WidebandAirFuelRatioResponse(rawResponse)
    }
}

class WidebandAirFuelRatioResponse(rawResponse: String) : OBDResponse("WidebandAirFuelRatioResponse", rawResponse) {
    val wafr: Float

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        wafr = (((buffer[2] * 256) + buffer[3]) / 32768) * 14.7f//((A*256)+B)/32768
    }

    override fun getFormattedResult(): String {
        return "$wafr:1 AFR"
    }
}

