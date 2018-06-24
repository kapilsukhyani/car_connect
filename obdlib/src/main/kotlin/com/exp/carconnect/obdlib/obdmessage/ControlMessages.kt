package com.exp.carconnect.obdlib.obdmessage

import java.util.*
import java.util.regex.Pattern


class FreezeDTCRequest(retriable: Boolean = true,
                       repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(OBDRequestMode.FREEZE_FRAME,
                "FreezeDTCRequest", "02",
                retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return FreezeDTCResponse(rawResponse)
    }
}

class FreezeDTCResponse(rawResponse: String) : OBDResponse("FreezeDTCResponse", rawResponse) {
    val framesAvailable = false
}

class DistanceRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                      val distanceType: DistanceCommandType,
                      retriable: Boolean = true,
                      repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "DistanceRequest ${distanceType.name}", distanceType.value, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return DistanceResponse(distanceType, rawResponse)
    }
}

class DistanceResponse(val distanceType: DistanceCommandType, rawResponse: String) : OBDResponse("DistanceResponse", rawResponse) {
    val km: Int

    init {
        val buffer = rawResponse.toIntList()
        km = buffer[2] * 256 + buffer[3]
    }

    override fun getFormattedResult(): String {
        return "$km km"
    }
}


enum class DistanceCommandType private constructor(val value: String) {
    SINCE_MIL_ON("21"),
    SINCE_CC_CLEARED("31")
}

class DTCNumberRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                       retriable: Boolean = true,
                       repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "DTCNumberRequest", "01", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return DTCNumberResponse(rawResponse)
    }
}

class DTCNumberResponse(rawResponse: String) : OBDResponse("DTCNumberResponse", rawResponse) {
    val codeCount: Int
    val milOn: Boolean
    val motorType: MotorType
    val tests: Array<MonitorTest>

    init {
        val buffer = rawResponse.toIntList()
        milOn = buffer[2] and 0x80 == 128
        codeCount = buffer[2] and 0x7F
        motorType = if (buffer[3] and 0x08 == 8) {
            tests = Array(9, { index ->
                var available = false
                var complete = false
                when (index) {
                    0 -> {
                        available = (buffer[3] and 0x04 == 4)
                        complete = (buffer[3] and 0x40 == 0)
                        MonitorTest(TestType.COMPONENTS, available, complete)
                    }
                    1 -> {
                        available = (buffer[3] and 0x02 == 2)
                        complete = (buffer[3] and 0x20 == 0)
                        MonitorTest(TestType.FUEL_SYSTEM, available, complete)
                    }
                    2 -> {
                        available = (buffer[3] and 0x01 == 1)
                        complete = (buffer[3] and 0x10 == 0)
                        MonitorTest(TestType.MIS_FIRE, available, complete)
                    }
                    3 -> {
                        available = (buffer[4] and 0x80 == 128)
                        complete = (buffer[5] and 0x80 == 0)
                        MonitorTest(TestType.EGR_SYSTEM, available, complete)
                    }
                    4 -> {
                        available = (buffer[4] and 0x40 == 64)
                        complete = (buffer[5] and 0x40 == 0)
                        MonitorTest(TestType.PM_FILTER_MONITORING, available, complete)
                    }
                    5 -> {
                        available = (buffer[4] and 0x20 == 32)
                        complete = (buffer[5] and 0x20 == 0)
                        MonitorTest(TestType.EXHAUST_GAS_SENSOR, available, complete)
                    }
                    6 -> {
                        available = (buffer[4] and 0x08 == 8)
                        complete = (buffer[5] and 0x08 == 0)
                        MonitorTest(TestType.BOOST_PRESSURE, available, complete)
                    }
                    7 -> {
                        available = (buffer[4] and 0x02 == 2)
                        complete = (buffer[5] and 0x02 == 0)
                        MonitorTest(TestType.SCR_MONITOR, available, complete)
                    }
                    else -> {
                        available = (buffer[4] and 0x01 == 1)
                        complete = (buffer[5] and 0x01 == 0)
                        MonitorTest(TestType.NMHC_CATALYST, available, complete)
                    }

                }

            })
            MotorType.COMPRESSION

        } else {
            tests = Array(11, { index ->
                var available = false
                var complete = false
                when (index) {
                    0 -> {
                        available = (buffer[3] and 0x04 == 4)
                        complete = (buffer[3] and 0x40 == 0)
                        MonitorTest(TestType.COMPONENTS, available, complete)
                    }
                    1 -> {
                        available = (buffer[3] and 0x02 == 2)
                        complete = (buffer[3] and 0x20 == 0)
                        MonitorTest(TestType.FUEL_SYSTEM, available, complete)
                    }
                    2 -> {
                        available = (buffer[3] and 0x01 == 1)
                        complete = (buffer[3] and 0x10 == 0)
                        MonitorTest(TestType.MIS_FIRE, available, complete)
                    }
                    3 -> {
                        available = (buffer[4] and 0x80 == 128)
                        complete = (buffer[5] and 0x80 == 0)
                        MonitorTest(TestType.EGR_SYSTEM, available, complete)
                    }
                    4 -> {
                        available = (buffer[4] and 0x40 == 64)
                        complete = (buffer[5] and 0x40 == 0)
                        MonitorTest(TestType.OXYGEN_SENSOR_HEATER, available, complete)
                    }
                    5 -> {
                        available = (buffer[4] and 0x20 == 32)
                        complete = (buffer[5] and 0x20 == 0)
                        MonitorTest(TestType.OXYGEN_SENSOR, available, complete)
                    }
                    6 -> {
                        available = (buffer[4] and 0x10 == 16)
                        complete = (buffer[5] and 0x10 == 0)
                        MonitorTest(TestType.AC_REFRIGERANT, available, complete)
                    }

                    7 -> {
                        available = (buffer[4] and 0x08 == 8)
                        complete = (buffer[5] and 0x08 == 0)
                        MonitorTest(TestType.SECONDARY_AIR_SYSTEM, available, complete)
                    }

                    8 -> {
                        available = (buffer[4] and 0x04 == 4)
                        complete = (buffer[5] and 0x04 == 0)
                        MonitorTest(TestType.EVAPORATIVE_SYSTEM, available, complete)
                    }
                    9 -> {
                        available = (buffer[4] and 0x02 == 2)
                        complete = (buffer[5] and 0x02 == 0)
                        MonitorTest(TestType.HEATED_CATALYST, available, complete)
                    }
                    else -> {
                        available = (buffer[4] and 0x01 == 1)
                        complete = (buffer[5] and 0x01 == 0)
                        MonitorTest(TestType.CATALYST, available, complete)
                    }

                }
            })
            MotorType.SPARK
        }

    }

    override fun getFormattedResult(): String {
        val res = if (milOn) "MIL is ON" else "MIL is OFF"
        return "$res  $codeCount codes"
    }
}

enum class MotorType {
    SPARK,
    COMPRESSION
}

enum class TestType {
    COMPONENTS,
    FUEL_SYSTEM,
    MIS_FIRE,
    EGR_SYSTEM,
    OXYGEN_SENSOR_HEATER,
    OXYGEN_SENSOR,
    AC_REFRIGERANT,
    SECONDARY_AIR_SYSTEM,
    EVAPORATIVE_SYSTEM,
    HEATED_CATALYST,
    CATALYST,
    PM_FILTER_MONITORING,
    EXHAUST_GAS_SENSOR,
    BOOST_PRESSURE,
    SCR_MONITOR,
    NMHC_CATALYST
}

data class MonitorTest(val testType: TestType, val available: Boolean, val complete: Boolean)


class EquivalentRatioRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                             retriable: Boolean = true,
                             repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "EquivalentRatioRequest", "44", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return EquivalentRatioResponse(rawResponse)
    }
}

class EquivalentRatioResponse(rawResponse: String) : OBDResponse("EquivalentRatioResponse", rawResponse) {
    val ratio: Float

    init {
        val buffer = rawResponse.toIntList()
        // ignore first two bytes [hh hh] of the response
        ratio = (buffer[2] * 256.0f + buffer[3]) / 32768.0f
    }

    override fun getFormattedResult(): String {
        return "$ratio  %"
    }
}


class IgnitionMonitorRequest(retriable: Boolean = true,
                             repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("IgnitionMonitorRequest", "AT IGN", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return IgnitionMonitorResponse(rawResponse)
    }
}

class IgnitionMonitorResponse(rawResponse: String) : OBDResponse("IgnitionMonitorResponse", rawResponse) {
    val ignitionOn = rawResponse.equals("ON", true)
    override fun getFormattedResult(): String {
        return rawResponse
    }
}

class ModuleVoltageRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                           retriable: Boolean = true,
                           repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "ModuleVoltageRequest", "42", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return ModuleVoltageResponse(rawResponse)
    }
}

class ModuleVoltageResponse(rawResponse: String) : OBDResponse("ModuleVoltageResponse", rawResponse) {
    val voltage: Float

    init {
        val buffer = rawResponse.toIntList()
        // ignore first two bytes [hh hh] of the response
        voltage = (buffer[2] * 256.0f + buffer[3]) / 1000.0f
    }

    override fun getFormattedResult(): String {
        return "$voltage V"
    }
}


class PendingTroubleCodesRequest(commandType: TroubleCodeCommandType, retriable: Boolean = true,
                                 repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("PendingTroubleCodesRequest ${commandType.name}", commandType.value, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return PendingTroubleCodesResponse(rawResponse)
    }
}

enum class TroubleCodeCommandType(val value: String) {
    PENDING("07"),
    PERMANENT("0A"),
    ALL("03")
}

class PendingTroubleCodesResponse(rawResponse: String) : OBDResponse("PendingTroubleCodesResponse", rawResponse) {
    val codes: List<String>

    companion object {
        /** Constant `dtcLetters={'P', 'C', 'B', 'U'}`  */
        @JvmStatic
        val dtcLetters = charArrayOf('P', 'C', 'B', 'U')
        /** Constant `hexArray="0123456789ABCDEF".toCharArray()`  */
        @JvmStatic
        val hexArray = "0123456789ABCDEF".toCharArray()
    }

    init {
        codes = parseDTCs()
    }

    /**
     * Sample DTCs response from simulator
     * 0100:4304016803031:000000000000002:000000000000003:00000000000000
     * 0100:4302010100001:000000000000002:000000000000003:00000000000000
     * 0100:4302010100001:000000000000002:000000000000003:00000000000000(p0101)
     * 0100:4302502F00001:000000000000002:000000000000003:00000000000000(c102f)
     * 0100:4304502F01011:000000000000002:000000000000003:00000000000000(c102f,p0101)
     * 0100:4308502F01011:010201030000002:000000000000003:00000000000000(c102f,p0101,p0102,p0103)
     * 0100:430E502F01011:01020103502EC12:01A201000000013:00000000000000(c102f,p0101,p0102,p0103,c102e,u0101,b2201)
     *
     * https://en.wikipedia.org/wiki/ISO_15765-2
     * https://en.wikipedia.org/wiki/OBD-II_PIDs#Mode_3_(no_PID_required)
     */
    private fun parseDTCs(): List<String> {
        val dtcs = mutableListOf<String>()
        val workingData: String
        var startIndex = 0//Header size.

        val canOneFrame = rawResponse.replace("[\r\n]".toRegex(), "")
        val canOneFrameLength = canOneFrame.length
        if (canOneFrameLength <= 16 && canOneFrameLength % 4 == 0) {//CAN(ISO-15765) protocol one frame.
            workingData = canOneFrame//47yy{codes}
            startIndex = 4//Header is 47yy, yy showing the number of data items.
        } else if (rawResponse.contains(":")) {//CAN(ISO-15765) protocol two and more frames.
            workingData = rawResponse.replace("[\r\n[0-3]?]?:".toRegex(), "")//xxx47yy{codes}
            startIndex = 7//Header is xxx47yy, xxx is bytes of information to follow, yy showing the number of data items.
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            workingData = rawResponse.replace("^47|[\r\n]47|[\r\n]".toRegex(), "")
        }
        var begin = startIndex

        while (begin < workingData.length) {
            var dtc = ""
            val b1 = hexStringToByteArray(workingData[begin])
            val ch1 = b1 and 0xC0 shr (6)
            val ch2 = b1 and 0x30 shr (4)
            dtc += dtcLetters[ch1]
            dtc += hexArray[ch2]
            dtc += workingData.substring(begin + 1, begin + 4)
            if (dtc == "P0000") {
                break
            }
            dtcs.add(dtc)
            begin += 4
        }

        return Collections.unmodifiableList(dtcs)
    }

    private fun hexStringToByteArray(s: Char): Int {
        return (Character.digit(s, 16) shl 4)
    }

    override fun getFormattedResult(): String {
        return codes.toString()
    }
}


class TimingAdvanceRequest(mode: OBDRequestMode = OBDRequestMode.CURRENT,
                           retriable: Boolean = true,
                           repeatable: IsRepeatable = IsRepeatable.No) :
        MultiModeOBDRequest(mode, "TimingAdvanceRequest", "0E", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return TimingAdvanceResponse(rawResponse)
    }
}

class TimingAdvanceResponse(rawResponse: String) : OBDResponse("TimingAdvanceResponse", rawResponse) {
    val timingAdvance: Float

    init {
        val buffer = rawResponse.toIntList()
        // ignore first two bytes [hh hh] of the response
        timingAdvance = (buffer[2] * 100.0f) / 255.0f
    }

    override fun getFormattedResult(): String {
        return "$timingAdvance %"
    }
}


class VinRequest(retriable: Boolean = true) :
        OBDRequest("VinRequest", "09 02", retriable, IsRepeatable.No, true) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return VinResponse(rawResponse)
    }
}

class VinResponse(rawResponse: String) : OBDResponse("VinResponse", rawResponse) {
    companion object {
        val pattern: Pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE)
    }

    val vin: String

    init {
        val result = rawResponse
        var workingData: String
        if (result.contains(":")) {//CAN(ISO-15765) protocol.
            workingData = result.replace(".:".toRegex(), "").substring(9)//9 is xxx490201, xxx is bytes of information to follow.
            val m = pattern.matcher(workingData.hexToString())
            if (m.find()) workingData = result.replace("0:49".toRegex(), "").replace(".:".toRegex(), "")
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            workingData = result.replace("49020.".toRegex(), "")
        }
        vin = workingData.hexToString().replace("[\u0000-\u001f]".toRegex(), "")
    }

    override fun getFormattedResult(): String {
        return vin
    }
}