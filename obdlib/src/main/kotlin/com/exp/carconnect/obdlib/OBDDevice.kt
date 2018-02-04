package com.exp.carconnect.obdlib

import io.reactivex.Observable
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern


internal class OBDDevice(private val inputStream: InputStream, private val outputStream: OutputStream) : IOBDDevice {

    companion object {
        private val TAG: String = "OBDDevice"
        private val WHITESPACE_PATTERN = Pattern.compile("\\s")
        private val BUSINIT_PATTERN = Pattern.compile("(BUS INIT)|(BUSINIT)|(\\.)")
        private val SEARCHING_PATTERN = Pattern.compile("SEARCHING")
        fun removeAll(pattern: Pattern, input: String): String {
            return pattern.matcher(input).replaceAll("")
        }
    }

    private val latestRequestResponseSnapShot = mutableMapOf<String, String>()

    override fun run(command: String, returnCachedResponse: Boolean): Observable<String> {
        return Observable.defer {
            try {
                if (returnCachedResponse &&
                        latestRequestResponseSnapShot.containsKey(command)) {
                    Logger.log(TAG, "[ $command  ] returning cached response [  ${latestRequestResponseSnapShot[command]} ]")
                    return@defer Observable.just(latestRequestResponseSnapShot[command])
                }
                val startTime = System.currentTimeMillis()
                writeCommand(command)
                val response = readResponse(command)
                val endTime = System.currentTimeMillis()
                Logger.log(TAG, "[ $command  ] executed in [  ${endTime - startTime}  ]ms")
                latestRequestResponseSnapShot.put(command, response)
                Observable.just(response)
            } catch (e: Exception) {
                Observable.error<String>(e)
            }
        }
    }


    override fun getLatestResponse(command: String): Observable<String?> {
        return Observable.just(latestRequestResponseSnapShot[command])
    }

    override fun purgeCachedResponseFor(command: String) {
        latestRequestResponseSnapShot.remove(command)
    }

    override fun purgeAllCachedResponses() {
        latestRequestResponseSnapShot.clear()
    }

    private fun writeCommand(command: String) {

        // write to OutputStream (i.e.: a BluetoothSocket) with an added
        // Carriage return
        outputStream.write((command + "\r").toByteArray())
        outputStream.flush()
    }

    private fun readResponse(command: String): String {
        var rawData: String
        var b: Byte
        val res = StringBuilder()

        // read until '>' arrives OR end of stream reached
        var c: Char
        // -1 if the end of the stream is reached
        while (true) {
            b = inputStream.read().toByte()
            if (b.equals(-1)) {
                break
            }
            c = b.toChar()
            if (c == '>')
            // read until '>' arrives
            {
                break
            }
            res.append(c)
        }


        /*
     * Imagine the following response 41 0c 00 0d.
     *
     * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
     * attention to the fact that I've put the word byte in quotes, because 41
     * is actually TWO bytes (two chars) in the socket. So, we must do some more
     * processing..
     */
        rawData = removeAll(SEARCHING_PATTERN, res.toString())

        /*
     * Data may have echo or informative text like "INIT BUS..." or similar.
     * The response ends with two carriage return characters. So we need to take
     * everything from the last carriage return before those two (trimmed above).
     */
        //kills multiline.. rawData = rawData.substring(rawData.lastIndexOf(13) + 1);
        rawData = removeAll(WHITESPACE_PATTERN, rawData)//removes all [ \t\n\x0B\f\r]

        BadResponseException.throwExceptionIfAny(command, rawData)

        rawData = removeAll(BUSINIT_PATTERN, rawData)

        return rawData
    }


}

abstract class BadResponseException(private val command: String, private val response: String) : RuntimeException() {


    companion object {
        private val BUSINIT_ERROR_MESSAGE_PATTERN = "BUS INIT... ERROR"
        private val MISUNDERSTOOD_COMMAND_MESSAGE_PATTERN = "?"
        private val NO_DATE_MESSAGE_PATTERN = "NO DATA"
        private val STOPPED_MESSAGE_PATERN = "STOPPED"
        private val UNABLE_TO_CONNECT_MESSAGE_PATTERN = "UNABLE TO CONNECT"
        private val ERROR_MESSAGE_PATTERN = "ERROR"
        private val UNSUPPORTED_COMMAND_MESSAGE_PATTERN = Pattern.compile("7F 0[0-A] 1[1-2]")
        private fun String.removeSpacesAndCapitalize(): String {
            return this.replace("\\s".toRegex(), "").toUpperCase()
        }

        @Throws(BadResponseException::class)
        fun throwExceptionIfAny(command: String, response: String) {
            if (response.removeSpacesAndCapitalize().contains(BUSINIT_ERROR_MESSAGE_PATTERN.removeSpacesAndCapitalize())) {
                throw BusInitException(command, response)
            } else if (response
                    .removeSpacesAndCapitalize()
                    .contains(MISUNDERSTOOD_COMMAND_MESSAGE_PATTERN
                            .removeSpacesAndCapitalize())) {
                throw MisUnderstoodCommandException(command, response)
            } else if (response
                    .removeSpacesAndCapitalize()
                    .contains(NO_DATE_MESSAGE_PATTERN
                            .removeSpacesAndCapitalize())) {
                throw NoDataException(command, response)
            } else if (response
                    .removeSpacesAndCapitalize()
                    .contains(STOPPED_MESSAGE_PATERN
                            .removeSpacesAndCapitalize())) {
                throw StoppedException(command, response)
            } else if (response
                    .removeSpacesAndCapitalize()
                    .contains(UNABLE_TO_CONNECT_MESSAGE_PATTERN
                            .removeSpacesAndCapitalize())) {
                throw UnableToConnectException(command, response)
            } else if (response
                    .removeSpacesAndCapitalize()
                    .contains(ERROR_MESSAGE_PATTERN
                            .removeSpacesAndCapitalize())) {
                throw UnknownErrorException(command, response)
            } else if (response
                    .removeSpacesAndCapitalize()
                    .matches(UNSUPPORTED_COMMAND_MESSAGE_PATTERN.toRegex())) {
                throw UnSupportedCommandException(command, response)
            }
        }
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName} while executing command [$command], response [$response]"
    }
}

interface IOBDDevice {
    fun run(command: String, returnCachedResponse: Boolean = false): Observable<String>
    fun getLatestResponse(command: String): Observable<String?>
    fun purgeCachedResponseFor(command: String)
    fun purgeAllCachedResponses()
}

class BusInitException(command: String, response: String) : BadResponseException(command, response)
class MisUnderstoodCommandException(command: String, response: String) : BadResponseException(command, response)
class NoDataException(command: String, response: String) : BadResponseException(command, response)
class StoppedException(command: String, response: String) : BadResponseException(command, response)
class UnableToConnectException(command: String, response: String) : BadResponseException(command, response)
class UnknownErrorException(command: String, response: String) : BadResponseException(command, response)
class UnSupportedCommandException(command: String, response: String) : BadResponseException(command, response)


