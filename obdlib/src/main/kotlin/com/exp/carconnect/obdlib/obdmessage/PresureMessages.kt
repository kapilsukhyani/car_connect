package com.exp.carconnect.obdlib.obdmessage


class BarometricPressureRequest(retriable: Boolean = true,
                                repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("BarometricPressureRequest", "01 33", retriable, repeatable) {
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


class FuelPressureRequest(retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("FuelPressureRequest", "01 0A", retriable, repeatable) {
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


class FuelRailPressureRequest(retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("FuelRailPressureRequest", "01 23", retriable, repeatable) {
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



class IntakeManifoldPressureRequest(retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("IntakeManifoldPressureRequest", "01 0B", retriable, repeatable) {
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