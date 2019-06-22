package com.exp.carconnect.obdlib.obdmessage


class TemperatureRequest(val temperatureType: TemperatureType,
                         retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("TemperatureRequest", temperatureType.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return TemperatureResponse(rawResponse, temperatureType)
    }
}

class TemperatureResponse(val temperature: Int,
                          val type: TemperatureType,
                          rawResponse: String = "") : OBDResponse("TemperatureResponse", rawResponse) {

    constructor(rawResponse: String, type: TemperatureType) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val temperature = buffer[2] - 40
        temperature
    }(),
            type,
            rawResponse)

    override fun getFormattedResult(): String {
        return "$temperature C"
    }
}


enum class TemperatureType(val command: String) {
    AIR_INTAKE("01 0F"),
    AMBIENT_AIR("01 46"),
    ENGINE_COOLANT("01 05")
}


class CatalystTemperatureRequest(val temperatureType: CatalystTemperatureType,
                                 retriable: Boolean = true,
                                 repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(OBDRequestMode.CURRENT, "CatalystTemperatureRequest", temperatureType.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return CatalystTemperatureResponse(rawResponse, temperatureType)
    }
}

class CatalystTemperatureResponse(val temperature: Float,
                                  val type: CatalystTemperatureType,
                                  rawResponse: String = "") : OBDResponse("CatalystTemperatureResponse", rawResponse) {

    constructor(rawResponse: String, type: CatalystTemperatureType) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val temperature = (256.0f * buffer[2] + buffer[3]) / 10.0f - 40
        temperature
    }(),
            type,
            rawResponse)

    override fun getFormattedResult(): String {
        return "$temperature C"
    }
}


enum class CatalystTemperatureType(val command: String) {
    BANK1SEONSOR1("3C"),
    BANK2SEONSOR1("3D"),
    BANK1SEONSOR2("3E"),
    BANK2SEONSOR2("3F")
}