package com.exp.carconnect.obdlib.obdmessage


class RPMRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                 retriable: Boolean = true,
                 repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RPMRequest", "0C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): RPMResponse {
        return RPMResponse(rawResponse)
    }
}

class RPMResponse(val rpm: Int,
                  rawResponse: String = "") : OBDResponse("RPMResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val rpm = (buffer[2] * 256 + buffer[3]) / 4
        rpm
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$rpm RPM"
    }
}

class AbsoluteLoadRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                          retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "AbsoluteLoadRequest", "43", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return AbsoluteLoadResponse(rawResponse)
    }
}


class AbsoluteLoadResponse(val ratio: Float, rawResponse: String = "") :
        OBDResponse("AbsoluteLoadResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        val b = buffer[3]
        val ratio = (a * 256 + b) * 100.0f / 255.0f
        ratio
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$ratio %"
    }
}


class CommandedEGRRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                          retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "CommandedEGRRequest", "2C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return CommandedEGRResponse(rawResponse)
    }
}


class CommandedEGRResponse(val ratio: Float, rawResponse: String = "") :
        OBDResponse("CommandedEGRResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        val ratio = (a) * 100.0f / 255.0f
        ratio
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$ratio %"
    }
}


class CommandedEGRErrorRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                               retriable: Boolean = true,
                               repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "CommandedEGRErrorRequest", "2D", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return CommandedEGRErrorResponse(rawResponse)
    }
}


class CommandedEGRErrorResponse(val error: Float, rawResponse: String = "") :
        OBDResponse("CommandedEGRErrorResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        val error = ((a) * 100.0f / 128.0f) - 100
        error
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$error %"
    }
}


class CommandedEvaporativePurgeRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                       retriable: Boolean = true,
                                       repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "CommandedEvaporativePurgeRequest", "2E", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return CommandedEvaporativePurgeResponse(rawResponse)
    }
}


class CommandedEvaporativePurgeResponse(val ratio: Float, rawResponse: String = "") :
        OBDResponse("CommandedEvaporativePurgeResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        val ratio = (a) * 100.0f / 255.0f
        ratio
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$ratio %"
    }
}


class WarmupsSinceCodeClearedRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                     retriable: Boolean = true,
                                     repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "WramupsSinceCodeClearedRequest", "30", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return WarmupsSinceCodeClearedResponse(rawResponse)
    }
}


class WarmupsSinceCodeClearedResponse(val warmUps: Int, rawResponse: String = "") :
        OBDResponse("WramupsSinceCodeClearedResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        val warmUps = (a)
        warmUps
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$warmUps"
    }
}


class LoadRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                  retriable: Boolean = true,
                  repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "LoadRequest", "04", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return LoadResponse(rawResponse)
    }
}

class LoadResponse(val load: Float, rawResponse: String = "") :
        OBDResponse("LoadResponse", rawResponse) {
    constructor(rawResponse: String) : this((rawResponse.toIntList()[2] * 100.0f) / 255.0f, rawResponse)

    override fun getFormattedResult(): String {
        return "$load %"
    }
}


class MassAirFlowRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                         retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "MassAirFlowRequest", "10", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return MassAirFlowResponse(rawResponse)
    }
}

class MassAirFlowResponse(val maf: Float, rawResponse: String = "") :
        OBDResponse("MassAirFlowResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        val maf = (buffer[2] * 256 + buffer[3]) / 100.0f
        maf
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$maf g/s"
    }
}


class OilTempRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                     retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "OilTempRequest", "5C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return OilTempResponse(rawResponse)
    }
}

class OilTempResponse(val temperature: Int, rawResponse: String = "") :
        OBDResponse("OilTempResponse", rawResponse) {
    constructor(rawResponse: String) : this(rawResponse.toIntList()[2] - 40, rawResponse)

    override fun getFormattedResult(): String {
        return "$temperature C"
    }
}


class RuntimeRequest(val type: RuntimeType, mode: OBDRequestMode = OBDRequestMode.CURRENT,
                     retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RuntimeRequest", type.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return RuntimeResponse(rawResponse, type)
    }
}

class RuntimeResponse(val value: Int,
                      val type: RuntimeType,
                      rawResponse: String = "") :
        OBDResponse("RuntimeResponse", rawResponse) {
    constructor(rawResponse: String, type: RuntimeType) : this({
        val buffer = rawResponse.toIntList()
        val value = buffer[2] * 256 + buffer[3]
        value
    }(),
            type,
            rawResponse)

    override fun getFormattedResult(): String {
        val hh = String.format("%02d", value / 3600)
        val mm = String.format("%02d", value % 3600 / 60)
        val ss = String.format("%02d", value % 60)
        return "$hh:$mm:$ss"
    }
}

enum class RuntimeType(val command: String) {
    SINCE_ENGINE_START("1F"),
    WITH_MIL_ON("4D"),
    SINCE_DTC_CLEARED("4E")
}


class ThrottlePositionRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                              retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "ThrottlePositionRequest", "11", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return ThrottlePositionResponse(rawResponse)
    }
}

class ThrottlePositionResponse(val throttle: Float, rawResponse: String = "") :
        OBDResponse("ThrottlePositionResponse", rawResponse) {
    constructor(rawResponse: String) : this((rawResponse.toIntList()[2] * 100.0f) / 255.0f, rawResponse)

    override fun getFormattedResult(): String {
        return "$throttle %"
    }
}


class ThrottleRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                      val type: ThrottleRequestType = ThrottleRequestType.RELATIVE_POSITION,
                      retriable: Boolean = true,
                      repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "ThrottleRequest", type.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return ThrottleResponse(rawResponse, type)
    }
}

class ThrottleResponse(val response: Float, val type: ThrottleRequestType, rawResponse: String = "") :
        OBDResponse("ThrottleResponse", rawResponse) {
    constructor(rawResponse: String, type: ThrottleRequestType) : this((rawResponse.toIntList()[2] * 100.0f) / 255.0f, type, rawResponse)

    override fun getFormattedResult(): String {
        return "$response %"
    }
}

enum class ThrottleRequestType(val command: String) {
    RELATIVE_POSITION("45"),
    ABSOLUTE_POSITION_B("47"),
    ABSOLUTE_POSITION_C("48"),
    ACCELERATOR_PEDAL_POSITION_D("49"),
    ACCELERATOR_PEDAL_POSITION_E("4A"),
    ACCELERATOR_PEDAL_POSITION_F("4B"),
    RELATIVE_ACCELERATOR_PEDAL_POSITION("5A"),
    COMMANDED_THROTTLE_ACTUATOR("4C")

}

class SpeedRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                   retriable: Boolean = true,
                   repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "SpeedRequest", "0D", retriable, repeatable) {

    override fun toResponse(rawResponse: String): OBDResponse {
        return SpeedResponse(rawResponse)
    }
}

class SpeedResponse(val metricSpeed: Int,
                    rawResponse: String = "") : OBDResponse("SpeedResponse", rawResponse) {
    constructor(rawResponse: String) : this(rawResponse.toIntList()[2], rawResponse)

    override fun getFormattedResult(): String {
        return "$metricSpeed km/h"
    }
}