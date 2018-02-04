package com.exp.carconnect.obdlib.obdmessage


class RPMRequest(retriable: Boolean = true,
                 repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("RPMRequest", "01 0C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): RPMResponse {
        return RPMResponse(rawResponse)
    }
}

class RPMResponse(rawResponse: String) : OBDResponse("RPMResponse", rawResponse) {
    val rpm: Int

    init {
        val buffer = rawResponse.toIntList()
        rpm = (buffer[2] * 256 + buffer[3]) / 4
    }

    override fun getFormattedResult(): String {
        return "$rpm RPM"
    }
}

class AbsoluteLoadRequest(retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("AbsoluteLoadRequest", "01 43", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return AbsoluteLoadResponse(rawResponse)
    }
}


class AbsoluteLoadResponse(rawResponse: String) :
        OBDResponse("AbsoluteLoadResponse", rawResponse) {

    val ratio: Float

    init {
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        val b = buffer[3]
        ratio = (a * 256 + b) * 100.0f / 255.0f
    }

    override fun getFormattedResult(): String {
        return "$ratio %"
    }
}

class LoadRequest(retriable: Boolean = true,
                  repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("LoadRequest", "01 04", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return LoadResponse(rawResponse)
    }
}

class LoadResponse(rawResponse: String) :
        OBDResponse("LoadResponse", rawResponse) {

    val load = (rawResponse.toIntList()[2] * 100.0f) / 255.0f

    override fun getFormattedResult(): String {
        return "$load %"
    }
}


class MassAirFlowRequest(retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("MassAirFlowRequest", "01 10", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return MassAirFlowResponse(rawResponse)
    }
}

class MassAirFlowResponse(rawResponse: String) :
        OBDResponse("MassAirFlowResponse", rawResponse) {

    val maf: Float

    init {
        val buffer = rawResponse.toIntList()
        maf = (buffer[2] * 256 + buffer[3]) / 100.0f
    }

    override fun getFormattedResult(): String {
        return "$maf g/s"
    }
}


class OilTempRequest(retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("OilTempRequest", "01 5C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return OilTempResponse(rawResponse)
    }
}

class OilTempResponse(rawResponse: String) :
        OBDResponse("OilTempResponse", rawResponse) {

    val temperature = rawResponse.toIntList()[2] - 40

    override fun getFormattedResult(): String {
        return "$temperature C"
    }
}


class RuntimeRequest(retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("RuntimeRequest", "01 1F", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return RuntimeResponse(rawResponse)
    }
}

class RuntimeResponse(rawResponse: String) :
        OBDResponse("RuntimeResponse", rawResponse) {

    val value: Int

    init {
        val buffer = rawResponse.toIntList()
        value = buffer[2] * 256 + buffer[3]
    }

    override fun getFormattedResult(): String {
        val hh = String.format("%02d", value / 3600)
        val mm = String.format("%02d", value % 3600 / 60)
        val ss = String.format("%02d", value % 60)
        return "$hh:$mm:$ss"
    }
}


class ThrottlePositionRequest(retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("ThrottlePositionRequest", "01 11", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return ThrottlePositionResponse(rawResponse)
    }
}

class ThrottlePositionResponse(rawResponse: String) :
        OBDResponse("ThrottlePositionResponse", rawResponse) {

    val throttle = (rawResponse.toIntList()[2] * 100.0f) / 255.0f

    override fun getFormattedResult(): String {
        return "$throttle %"
    }
}

class SpeedRequest(retriable: Boolean = true,
                   repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("SpeedRequest", "01 0D", retriable, repeatable) {

    override fun toResponse(rawResponse: String): OBDResponse {
        return SpeedResponse(rawResponse)
    }
}

class SpeedResponse(rawResponse: String) : OBDResponse("SpeedResponse", rawResponse) {

    val metricSpeed = rawResponse.toIntList()[2]

    override fun getFormattedResult(): String {
        return "$metricSpeed km/h"
    }
}