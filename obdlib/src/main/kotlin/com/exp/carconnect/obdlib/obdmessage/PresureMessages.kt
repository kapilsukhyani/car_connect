package com.exp.carconnect.obdlib.obdmessage


class BarometricPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                retriable: Boolean = true,
                                repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "BarometricPressureRequest", "33", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return BarometricPressureResponse(rawResponse)
    }
}

class BarometricPressureResponse(val barometricPressure: Int,
                                 rawResponse: String = "") : OBDResponse("BarometricPressureResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val barometricPressure = buffer[2]
        barometricPressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$barometricPressure kPa"
    }
}


class FuelPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                          retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "FuelPressureRequest", "0A", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return FuelPressureResponse(rawResponse)
    }
}

class FuelPressureResponse(val fuelPressure: Int,
                           rawResponse: String = "") : OBDResponse("FuelPressureResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val fuelPressure = buffer[2] * 3
        fuelPressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$fuelPressure kPa"
    }
}


class FuelRailPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                              retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "FuelRailPressureRequest", "23", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return FuelRailPressureResponse(rawResponse)
    }
}

class FuelRailPressureResponse(val fuelRailPressure: Int,
                               rawResponse: String = "") : OBDResponse("FuelRailPressureResponse", rawResponse) {
    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val fuelRailPressure = ((buffer[2] * 256) + buffer[3]) * 10
        fuelRailPressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$fuelRailPressure kPa"
    }
}

class RelativeFuelRailPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                      retriable: Boolean = true,
                                      repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "RelativeFuelRailPressureRequest", "22", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return RelativeFuelRailPressureResponse(rawResponse)
    }
}

class RelativeFuelRailPressureResponse(val relativeFuelRailPressure: Float,
                                       rawResponse: String = "") : OBDResponse("RelativeFuelRailPressureResponse", rawResponse) {

    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val relativeFuelRailPressure = ((buffer[2] * 256) + buffer[3]) * .079f
        relativeFuelRailPressure
    }(), rawResponse)


    override fun getFormattedResult(): String {
        return "$relativeFuelRailPressure kPa"
    }
}


class AbsoluteFuelRailPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                      retriable: Boolean = true,
                                      repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "AbsoluteFuelRailPressureRequest", "59", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return AbsoluteFuelRailPressureResponse(rawResponse)
    }
}

class AbsoluteFuelRailPressureResponse(val pressure: Float,
                                       rawResponse: String = "") : OBDResponse("AbsoluteFuelRailPressureResponse", rawResponse) {

    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val pressure = ((buffer[2] * 256) + buffer[3]) * 10.0f
        pressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$pressure kPa"
    }
}


class IntakeManifoldPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                    retriable: Boolean = true,
                                    repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "IntakeManifoldPressureRequest", "0B", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return IntakeManifoldPressureResponse(rawResponse)
    }
}

class IntakeManifoldPressureResponse(val intakeManifoldPressure: Int,
                                     rawResponse: String = "") : OBDResponse("IntakeManifoldPressureResponse", rawResponse) {

    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val intakeManifoldPressure = buffer[2]
        intakeManifoldPressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$intakeManifoldPressure kPa"
    }
}

class AbsoluteEvapSystemPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                        retriable: Boolean = true,
                                        repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "AbsoluteEvapSystempPressureRequest", "53", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return AbsoluteEvapSystemPressureResponse(rawResponse)
    }
}

class AbsoluteEvapSystemPressureResponse(val pressure: Float,
                                         rawResponse: String = "") : OBDResponse("AbsoluteEvapSystempPressureResponse", rawResponse) {

    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val pressure = (buffer[2] * 256.0f + buffer[3]) / 200.0f
        pressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$pressure kPa"
    }
}


class EvapSystemPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                retriable: Boolean = true,
                                repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "EvapSystemPressureRequest", "54", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return EvapSystemPressureResponse(rawResponse)
    }
}

class EvapSystemPressureResponse(val pressure: Float,
                                 rawResponse: String = "") : OBDResponse("EvapSystemPressureResponse", rawResponse) {

    constructor(rawResponse: String) : this({
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        val pressure = ((buffer[2] * 256.0f + buffer[3])) - 32767
        pressure
    }(),
            rawResponse)

    override fun getFormattedResult(): String {
        return "$pressure Pa"
    }
}