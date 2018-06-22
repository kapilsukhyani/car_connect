package com.exp.carconnect.obdlib.obdmessage


class BarometricPressureRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                                retriable: Boolean = true,
                                repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "BarometricPressureRequest", "33", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return BarometricPressureResponse(rawResponse)
    }
}

class BarometricPressureResponse(rawResponse: String) : OBDResponse("BarometricPressureResponse", rawResponse) {
    val barometricPressure: Int

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        barometricPressure = buffer[2]
    }

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

class FuelPressureResponse(rawResponse: String) : OBDResponse("FuelPressureResponse", rawResponse) {
    val fuelPressure: Int

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        fuelPressure = buffer[2] * 3
    }

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

class FuelRailPressureResponse(rawResponse: String) : OBDResponse("FuelRailPressureResponse", rawResponse) {
    val fuelRailPressure: Int

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        fuelRailPressure = ((buffer[2] * 256) + buffer[3]) * 10
    }

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

class RelativeFuelRailPressureResponse(rawResponse: String) : OBDResponse("RelativeFuelRailPressureResponse", rawResponse) {
    val relativeFuelRailPressure: Float

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        relativeFuelRailPressure = ((buffer[2] * 256) + buffer[3]) * .079f
    }

    override fun getFormattedResult(): String {
        return "$relativeFuelRailPressure kPa"
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

class IntakeManifoldPressureResponse(rawResponse: String) : OBDResponse("IntakeManifoldPressureResponse", rawResponse) {
    val intakeManifoldPressure: Int

    init {
        val buffer = rawResponse.toIntList()
        // // ignore first two bytes [hh hh] of the response
        intakeManifoldPressure = buffer[2]
    }

    override fun getFormattedResult(): String {
        return "$intakeManifoldPressure kPa"
    }
}