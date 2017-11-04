package com.exp.carconnect.basic.obdmessage


class TemperatureRequest(temperatureType: TemperatureType,
                         retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("TemperatureRequest", temperatureType.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return TemperatureResponse(rawResponse)
    }
}

class TemperatureResponse(rawResponse: String) : OBDResponse("TemperatureResponse", rawResponse) {
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