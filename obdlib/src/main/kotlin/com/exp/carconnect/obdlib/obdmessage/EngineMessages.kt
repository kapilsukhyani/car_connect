package com.exp.carconnect.obdlib.obdmessage


class RPMRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                 retriable: Boolean = true,
                 repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RPMRequest", "0C", retriable, repeatable) {
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

class AbsoluteLoadRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                          retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "AbsoluteLoadRequest", "43", retriable, repeatable) {
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


class CommandedEGRRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                          retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "CommandedEGRRequest", "2C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return CommandedEGRResponse(rawResponse)
    }
}


class CommandedEGRResponse(rawResponse: String) :
        OBDResponse("CommandedEGRResponse", rawResponse) {

    val ratio: Float

    init {
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        ratio = (a) * 100.0f / 255.0f
    }

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


class CommandedEGRErrorResponse(rawResponse: String) :
        OBDResponse("CommandedEGRErrorResponse", rawResponse) {

    val ratio: Float

    init {
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        ratio = (a) * 100.0f / 255.0f - 100
    }

    override fun getFormattedResult(): String {
        return "$ratio %"
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


class CommandedEvaporativePurgeResponse(rawResponse: String) :
        OBDResponse("CommandedEvaporativePurgeResponse", rawResponse) {

    val ratio: Float

    init {
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        ratio = (a) * 100.0f / 255.0f
    }

    override fun getFormattedResult(): String {
        return "$ratio %"
    }
}


class WramupsSinceCodeClearedRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                     retriable: Boolean = true,
                                     repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "WramupsSinceCodeClearedRequest", "30", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return WramupsSinceCodeClearedResponse(rawResponse)
    }
}


class WramupsSinceCodeClearedResponse(rawResponse: String) :
        OBDResponse("WramupsSinceCodeClearedResponse", rawResponse) {

    val warmUps: Int

    init {
        val buffer = rawResponse.toIntList()
        val a = buffer[2]
        warmUps = (a)
    }

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

class LoadResponse(rawResponse: String) :
        OBDResponse("LoadResponse", rawResponse) {

    val load = (rawResponse.toIntList()[2] * 100.0f) / 255.0f

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


class OilTempRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                     retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "OilTempRequest", "5C", retriable, repeatable) {
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


class RuntimeRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                     retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RuntimeRequest", "1F", retriable, repeatable) {
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

class RuntimeWithMILOnRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                              retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RuntimeWithMILOnRequest", "4D", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return RuntimeWithMILOnResponse(rawResponse)
    }
}

class RuntimeWithMILOnResponse(rawResponse: String) :
        OBDResponse("RuntimeWithMILOnResponse", rawResponse) {

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

class RuntimeSinceTroubleCodeClearedRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                            retriable: Boolean = true,
                                            repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RuntimeSinceTroubleCodeClearedRequest", "4E", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return RuntimeSinceTroubleCodeClearedResponse(rawResponse)
    }
}

class RuntimeSinceTroubleCodeClearedResponse(rawResponse: String) :
        OBDResponse("RuntimeSinceTroubleCodeClearedResponse", rawResponse) {

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


class ThrottlePositionRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                              retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "ThrottlePositionRequest", "11", retriable, repeatable) {
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


class ThrottleRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                      val type: ThrottleRequestType = ThrottleRequestType.RELATIVE_POSITION,
                      retriable: Boolean = true,
                      repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "ThrottleRequest", type.command, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return ThrottleResponse(rawResponse, type)
    }
}

class ThrottleResponse(rawResponse: String, type: ThrottleRequestType) :
        OBDResponse("ThrottleResponse", rawResponse) {

    val throttle = (rawResponse.toIntList()[2] * 100.0f) / 255.0f

    override fun getFormattedResult(): String {
        return "$throttle %"
    }
}

enum class ThrottleRequestType(val command: String) {
    RELATIVE_POSITION("45"),
    ABSOLUTE_POSITION_B("47"),
    ABSOLUTE_POSITION_C("48"),
    ACCELERATOR_PEDAL_POSITION_D("49"),
    ACCELERATOR_PEDAL_POSITION_E("4A"),
    ACCELERATOR_PEDAL_POSITION_F("4B"),
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

class SpeedResponse(rawResponse: String) : OBDResponse("SpeedResponse", rawResponse) {

    val metricSpeed = rawResponse.toIntList()[2]

    override fun getFormattedResult(): String {
        return "$metricSpeed km/h"
    }
}