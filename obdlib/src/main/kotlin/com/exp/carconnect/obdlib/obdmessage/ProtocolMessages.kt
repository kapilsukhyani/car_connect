package com.exp.carconnect.obdlib.obdmessage


class AdaptiveTimingCommand(val mode: Int,
                            retriable: Boolean = true,
                            repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("AdaptiveTimingRequest", "AT AT" + mode, retriable, repeatable)

class EchoOffCommand(retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("EchoOffRequest", "AT E0", retriable, repeatable)

class LineFeedOffCommand(retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("LineFeedOffRequest", "AT L0", retriable, repeatable)

class SelectProtocolCommand(val protocol: ObdProtocol,
                            retriable: Boolean = true,
                            repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("SelectProtocolRequest", "AT SP " + protocol.value, retriable, repeatable)

class SpacesOffCommand(retriable: Boolean = true,
                       repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("SpacesOffRequest", "ATS0", retriable, repeatable)

class TimeoutCommand(val timeout: Int,
                     retriable: Boolean = true,
                     repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("TimeoutRequest", "AT ST " + Integer.toHexString(0xFF and timeout), retriable, repeatable)

class AvailablePidsCommand(val pidCommand: PidCommand,
                           retriable: Boolean = true,
                           repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("AvailablePidsRequest", pidCommand.value, retriable, repeatable) {
    override fun toResponse(rawResponse: String): AvailablePidsResponse {
        return AvailablePidsResponse(pidCommand, rawResponse)
    }
}

class AvailablePidsResponse(command: PidCommand, rawResponse: String) : OBDResponse("AvailablePidsResponse", rawResponse) {
    val availablePids: Set<String>

    init {
        val availablePidsMutable = mutableSetOf<String>()
        val buffer = rawResponse.toIntList().subList(2, 6)
        var pidNumber = command.getStartingPid()
        for (value in buffer) {
            var i = 128
            while (i >= 1) {
                val isSet = (value and i) == i
                if (isSet) {
                    availablePidsMutable.add(pidNumber.getHexString())
                }
                i = i / 2
                pidNumber++
            }
        }
        availablePids = availablePidsMutable

    }

    private fun Int.getHexString(): String {
        val hexString = this.toString(16)
        return if (hexString.length == 1) {
            "0$hexString"
        } else {
            hexString
        }
    }

    override fun getFormattedResult(): String {
        return rawResponse.substring(4)
    }
}

class CloseCommand(retriable: Boolean = true,
                   repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("CloseRequest", "AT PC", retriable, repeatable)

class DescribeProtocolCommand(retriable: Boolean = true,
                              repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("DescribeProtocolRequest", "AT DP", retriable, repeatable)

class HeadersOffCommand(retriable: Boolean = true,
                        repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("HeadersOffRequest", "ATH0", retriable, repeatable)

class ObdWarmstartCommand(retriable: Boolean = true,
                          repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("ObdWarmstartRequest", "AT WS", retriable, repeatable)

class ResetTroubleCodesCommand(retriable: Boolean = true,
                               repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("ResetTroubleCodesRequest", "04", retriable, repeatable)

class OBDResetCommand(retriable: Boolean = true,
                      repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("OBDResetCommand", "AT Z", retriable, repeatable)


class OBDStandardRequest(retriable: Boolean = true,
                         repeatable: IsRepeatable = IsRepeatable.No) : OBDRequest("OBDStandardRequest", "01 1C", retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return OBDStandardResponse(rawResponse)
    }
}

class OBDStandardResponse(rawResponse: String) : OBDResponse("OBDStandardResponse", rawResponse) {
    val standard: ObdStandard

    init {
        val standardType = rawResponse.toIntList()[2]
        standard = when (standardType) {
            1 -> {
                ObdStandard.CARB_OBD2
            }
            2 -> {
                ObdStandard.EPA_OBD
            }
            3 -> {
                ObdStandard.OBD_OBD2
            }
            4 -> {
                ObdStandard.OBD1
            }
            5 -> {
                ObdStandard.NOT_OBD_COMPLIANT
            }
            6 -> {
                ObdStandard.EOBD
            }
            7 -> {
                ObdStandard.EOBD_OBD2
            }
            8 -> {
                ObdStandard.EOBD_OBD
            }
            9 -> {
                ObdStandard.EOBD_OBD_OBD2
            }
            10 -> {
                ObdStandard.JOBD
            }
            11 -> {
                ObdStandard.JOBD_OBD2
            }
            12 -> {
                ObdStandard.JOBD_EOBD
            }
            13 -> {
                ObdStandard.JOBD_EOBD_OBD2
            }
            17 -> {
                ObdStandard.EMD
            }
            18 -> {
                ObdStandard.EMD_PLUS
            }
            19 -> {
                ObdStandard.HD_OBD_C
            }
            20 -> {
                ObdStandard.HD_OBD
            }
            21 -> {
                ObdStandard.WWH_OBD
            }
            23 -> {
                ObdStandard.HD_EOBD1
            }
            24 -> {
                ObdStandard.HD_EOBD1_N
            }
            25 -> {
                ObdStandard.HD_EOBD2
            }
            26 -> {
                ObdStandard.HD_EOBD2_N
            }
            28 -> {
                ObdStandard.OBD_BR_1
            }
            29 -> {
                ObdStandard.OBD_BR_2
            }
            30 -> {
                ObdStandard.KOBD
            }
            31 -> {
                ObdStandard.IOBD
            }
            32 -> {
                ObdStandard.IOBD2
            }
            33 -> {
                ObdStandard.HD_EOBD4
            }
            else -> {
                ObdStandard.UNKNOWN
            }
        }
    }


}

enum class PidCommand(val value: String) {
    ONE_TO_TWENTY("01 00") {
        override fun getStartingPid(): Int {
            return 1
        }
    },
    TWENTY_ONE_TO_FOURTY("01 20") {
        override fun getStartingPid(): Int {
            return 33
        }
    },
    FOURTY_ONE_TO_SIXTY("01 40") {
        override fun getStartingPid(): Int {
            return 65
        }
    };

    abstract fun getStartingPid(): Int

}

enum class ObdProtocol(
        val value: Char) {

    /**
     * Auto select protocol and save.
     */
    AUTO('0'),

    /**
     * 41.6 kbaud
     */
    SAE_J1850_PWM('1'),

    /**
     * 10.4 kbaud
     */
    SAE_J1850_VPW('2'),

    /**
     * 5 baud init
     */
    ISO_9141_2('3'),

    /**
     * 5 baud init
     */
    ISO_14230_4_KWP('4'),

    /**
     * Fast init
     */
    ISO_14230_4_KWP_FAST('5'),

    /**
     * 11 bit ID, 500 kbaud
     */
    ISO_15765_4_CAN('6'),

    /**
     * 29 bit ID, 500 kbaud
     */
    ISO_15765_4_CAN_B('7'),

    /**
     * 11 bit ID, 250 kbaud
     */
    ISO_15765_4_CAN_C('8'),

    /**
     * 29 bit ID, 250 kbaud
     */
    ISO_15765_4_CAN_D('9'),

    /**
     * 29 bit ID, 250 kbaud (user adjustable)
     */
    SAE_J1939_CAN('A'),

    /**
     * 11 bit ID (user adjustable), 125 kbaud (user adjustable)
     */
    USER1_CAN('B'),

    /**
     * 11 bit ID (user adjustable), 50 kbaud (user adjustable)
     */
    USER2_CAN('C')
}


//reference : https://en.wikipedia.org/wiki/OBD-II_PIDs#Mode_1_PID_1C
enum class ObdStandard {
    CARB_OBD2,
    EPA_OBD,
    OBD_OBD2,
    OBD1,
    NOT_OBD_COMPLIANT,
    EOBD,
    EOBD_OBD2,
    EOBD_OBD,
    EOBD_OBD_OBD2,
    JOBD,
    JOBD_OBD2,
    JOBD_EOBD,
    JOBD_EOBD_OBD2,
    EMD,
    EMD_PLUS,
    HD_OBD_C,
    HD_OBD,
    WWH_OBD,
    HD_EOBD1,
    HD_EOBD1_N,
    HD_EOBD2,
    HD_EOBD2_N,
    OBD_BR_1,
    OBD_BR_2,
    KOBD,
    IOBD,
    IOBD2,
    HD_EOBD4,
    UNKNOWN

}