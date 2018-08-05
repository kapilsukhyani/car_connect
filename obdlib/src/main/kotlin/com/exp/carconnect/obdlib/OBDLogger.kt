package com.exp.carconnect.obdlib

interface OBDLogger {
    companion object {
        fun log(tag: String, message: String) {
            loggerImpl?.apply {
                log(tag, message)
            }
        }

        fun log(tag: String, exception: Throwable) {
            loggerImpl?.apply {
                log(tag, exception)
            }
        }

        fun log(tag: String, message: String, exception: Throwable) {
            loggerImpl?.apply {
                log(tag, message, exception)
            }
        }

        var loggerImpl: OBDLogger? = null
        fun init(logger: OBDLogger) {
            loggerImpl = logger
        }
    }

    fun log(tag: String, message: String)

    fun log(tag: String, exception: Throwable)

    fun log(tag: String, message: String, exception: Throwable)
}
