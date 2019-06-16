package com.exp.carconnect.obdlib.obdmessage

import com.exp.carconnect.obdlib.*
import com.exp.carconnect.obdlib.obdmessage.NonAlphaNumericException.Companion.DIGITS_LETTERS_PATTERN
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Producer of OBDResponse i.e. out is present, will never consume any OBDResponse
 */
abstract class OBDRequest(val tag: String,
                          val command: String,
                          val retriable: Boolean = true,
                          val isRepeatable: IsRepeatable = IsRepeatable.No,
                          val returnCachedResponse: Boolean = false) : Executable {
    companion object {
        private const val MAX_ATTEMPTS = 2
    }

    override fun execute(device: IOBDDevice): Observable<OBDResponse> {
        return device.run(command, returnCachedResponse)
                .retry(MAX_ATTEMPTS.toLong()) { e ->
                    retriable && when (e) {
                        is StoppedException,
                        is UnableToConnectException,
                        is BusInitException,
                        is NoDataException,
                        is UnknownErrorException -> {
                            OBDLogger.log("[$tag]", "caught retriable exception [$e], retrying")
                            true
                        }
                        else -> false
                    }
                }
                .lift<String> { downStream ->
                    object : Observer<String> {
                        override fun onError(e: Throwable) {
                            when (e) {
                                is BadResponseException -> downStream.onError(ExecutionException(this@OBDRequest, e))
                                else -> downStream.onError(e)
                            }
                        }

                        override fun onNext(t: String) {
                            downStream.onNext(t)
                        }

                        override fun onComplete() {
                            downStream.onComplete()
                        }

                        override fun onSubscribe(d: Disposable) {
                            downStream.onSubscribe(d)
                        }

                    }
                }
                .map { rawResponse ->
                    OBDLogger.log("[$tag]", "raw response [$rawResponse]")
                    try {
                        toResponse(rawResponse)
                    } catch (e: NonAlphaNumericException) {
                        throw ExecutionException(this@OBDRequest, object : BadResponseException(command, e.response) {})
                    }
                }

    }

    protected open fun toResponse(rawResponse: String): OBDResponse {
        return RawResponse(rawResponse)
    }
}

abstract class MultiModeOBDRequest(mode: OBDRequestMode,
                                   tag: String,
                                   command: String,
                                   retriable: Boolean = true,
                                   isRepeatable: IsRepeatable = IsRepeatable.No,
                                   returnCachedResponse: Boolean = false) :
        OBDRequest(tag, "${mode.value} $command",
                retriable,
                isRepeatable,
                returnCachedResponse)

enum class OBDRequestMode(val value: String) {
    CURRENT("01"),
    FREEZE_FRAME("02")
}

sealed class IsRepeatable {
    object No : IsRepeatable()
    data class Yes(val frequency: Long,
                   val unit: TimeUnit) : IsRepeatable()
}

abstract class OBDResponse(val tag: String,
                           val rawResponse: String) {
    open fun getFormattedResult(): String {
        return rawResponse
    }
}

open class RawResponse(rawResponse: String) : OBDResponse("RawResponse", rawResponse)

internal fun String.toIntList(): List<Int> {
    if (!this.matches(DIGITS_LETTERS_PATTERN.toRegex())) {
        throw NonAlphaNumericException(this)
    }
    val buffer = ArrayList<Int>()
    // read string each two chars
    buffer.clear()
    var begin = 0
    var end = 2
    while (end <= this.length) {
        buffer.add(Integer.decode("0x" + this.substring(begin, end)))
        begin = end
        end += 2
    }
    return buffer
}


internal fun String.hexToString(): String {
    val sb = StringBuilder()
    //49204c6f7665204a617661 split into two characters 49, 20, 4c...
    var i = 0
    while (i < this.length - 1) {

        //grab the hex in pairs
        val output = this.substring(i, i + 2)
        //convert hex to decimal
        val decimal = Integer.parseInt(output, 16)
        //convert the decimal to character
        sb.append(decimal.toChar())
        i += 2
    }
    return sb.toString()
}


class NonAlphaNumericException(val response: String) : RuntimeException() {
    companion object {
        val DIGITS_LETTERS_PATTERN = Pattern.compile("([0-9A-F])+")
    }
}

