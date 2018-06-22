package com.exp.carconnect.obdlib.obdmessage


class TemperatureRequest(private val temperatureType: TemperatureType,
                         retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("TemperatureRequest", temperatureType.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return TemperatureResponse(rawResponse, temperatureType)
    }
}

class TemperatureResponse(rawResponse: String, val type: TemperatureType) : OBDResponse("TemperatureResponse", rawResponse) {
    val temperature: Int

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        temperature = buffer[2] - 40
    }

    override fun getFormattedResult(): String {
        return "$temperature C"
    }
}


enum class TemperatureType(val command: String) {
    AIR_INTAKE("01 0F"),
    AMBIENT_AIR("01 46"),
    ENGINE_COOLANT("01 05")
}


class CatalystTemperatureRequest(private val temperatureType: CatalystTemperatureType,
                                 retriable: Boolean = true,
                                 repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(OBDRequestMode.CURRENT, "CatalystTemperatureRequest", temperatureType.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return CatalystTemperatureResponse(rawResponse, temperatureType)
    }
}

class CatalystTemperatureResponse(rawResponse: String, val type: CatalystTemperatureType) : OBDResponse("CatalystTemperatureResponse", rawResponse) {
    val temperature: Float

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        temperature = (256.0f * buffer[2] + buffer[3]) / 10.0f - 40
    }

    override fun getFormattedResult(): String {
        return "$temperature C"
    }
}


enum class CatalystTemperatureType(val command: String) {
    BANK1SEONSOR1("3C"),
    BANK1SEONSOR2("3D"),
    BANK2SEONSOR1("3E"),
    BANK2SEONSOR2("3F")
}