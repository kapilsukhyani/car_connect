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

class AvailablePidsCommand(command: PidCommand,
                           retriable: Boolean = true,
                           repeatable: IsRepeatable = IsRepeatable.No) :
        OBDRequest("AvailablePidsRequest", command.value, retriable, repeatable) {
    override fun toResponse(rawResponse: String): OBDResponse {
        return object : RawResponse(rawResponse) {
            override fun getFormattedResult(): String {
                return rawResponse.substring(4)
            }
        }
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



enum class PidCommand (val value: String) {
    ONE_TO_TWENTY("01 00"),
    TWENTY_ONE_TO_FOURTY("01 20"),
    FOURTY_ONE_TO_SIXTY("01 40")

}

enum class ObdProtocol (
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