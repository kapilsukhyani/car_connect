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

class AvailablePidsResponse(val availablePids: Set<String>,
                            command: PidCommand,
                            rawResponse: String = "") : OBDResponse("AvailablePidsResponse", rawResponse) {
    companion object {
        private fun Int.getHexString(): String {
            val hexString = this.toString(16)
            return if (hexString.length == 1) {
                "0$hexString"
            } else {
                hexString
            }
        }
    }

    constructor(command: PidCommand, rawResponse: String) : this({
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
        availablePidsMutable
    }(),
            command,
            rawResponse)


    override fun getFormattedResult(): String {
        return availablePids.toString()
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

class OBDStandardResponse(val standard: OBDStandard,
                          rawResponse: String = "") : OBDResponse("OBDStandardResponse", rawResponse) {

    constructor(rawResponse: String) : this({
        val standardType = rawResponse.toIntList()[2]
        OBDStandard.fromValue(standardType)
    }(),
            rawResponse)
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
enum class OBDStandard(val value: Int) {
    CARB_OBD2(1),
    EPA_OBD(2),
    OBD_OBD2(3),
    OBD1(4),
    NOT_OBD_COMPLIANT(5),
    EOBD(6),
    EOBD_OBD2(7),
    EOBD_OBD(8),
    EOBD_OBD_OBD2(9),
    JOBD(10),
    JOBD_OBD2(11),
    JOBD_EOBD(12),
    JOBD_EOBD_OBD2(13),
    EMD(17),
    EMD_PLUS(18),
    HD_OBD_C(19),
    HD_OBD(20),
    WWH_OBD(21),
    HD_EOBD1(23),
    HD_EOBD1_N(24),
    HD_EOBD2(25),
    HD_EOBD2_N(26),
    OBD_BR_1(28),
    OBD_BR_2(29),
    KOBD(30),
    IOBD(31),
    IOBD2(32),
    HD_EOBD4(33),
    UNKNOWN(0);

    companion object {
        fun fromValue(value: Int): OBDStandard {
            return when (value) {
                1 -> {
                    OBDStandard.CARB_OBD2
                }
                2 -> {
                    OBDStandard.EPA_OBD
                }
                3 -> {
                    OBDStandard.OBD_OBD2
                }
                4 -> {
                    OBDStandard.OBD1
                }
                5 -> {
                    OBDStandard.NOT_OBD_COMPLIANT
                }
                6 -> {
                    OBDStandard.EOBD
                }
                7 -> {
                    OBDStandard.EOBD_OBD2
                }
                8 -> {
                    OBDStandard.EOBD_OBD
                }
                9 -> {
                    OBDStandard.EOBD_OBD_OBD2
                }
                10 -> {
                    OBDStandard.JOBD
                }
                11 -> {
                    OBDStandard.JOBD_OBD2
                }
                12 -> {
                    OBDStandard.JOBD_EOBD
                }
                13 -> {
                    OBDStandard.JOBD_EOBD_OBD2
                }
                17 -> {
                    OBDStandard.EMD
                }
                18 -> {
                    OBDStandard.EMD_PLUS
                }
                19 -> {
                    OBDStandard.HD_OBD_C
                }
                20 -> {
                    OBDStandard.HD_OBD
                }
                21 -> {
                    OBDStandard.WWH_OBD
                }
                23 -> {
                    OBDStandard.HD_EOBD1
                }
                24 -> {
                    OBDStandard.HD_EOBD1_N
                }
                25 -> {
                    OBDStandard.HD_EOBD2
                }
                26 -> {
                    OBDStandard.HD_EOBD2_N
                }
                28 -> {
                    OBDStandard.OBD_BR_1
                }
                29 -> {
                    OBDStandard.OBD_BR_2
                }
                30 -> {
                    OBDStandard.KOBD
                }
                31 -> {
                    OBDStandard.IOBD
                }
                32 -> {
                    OBDStandard.IOBD2
                }
                33 -> {
                    OBDStandard.HD_EOBD4
                }
                else -> {
                    OBDStandard.UNKNOWN
                }
            }
        }
    }


}