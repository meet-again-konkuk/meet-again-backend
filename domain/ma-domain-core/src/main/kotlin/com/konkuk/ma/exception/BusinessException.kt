package com.konkuk.ma.exception

import com.konkuk.ma.logger

abstract class BusinessException(
    message: String,
    dataMessage: String,
    cause: Throwable? = null,
    logLevel: LogLevel = LogLevel.ERROR
) : RuntimeException(message, cause) {

    enum class LogLevel {
        ERROR, WARN, INFO, DEBUG
    }

    init {
        val logMessage = "$message $dataMessage"

        when (logLevel) {
            LogLevel.ERROR -> logger.error(this) { logMessage }
            LogLevel.WARN -> logger.warn(this) { logMessage }
            LogLevel.INFO -> logger.info { logMessage }
            LogLevel.DEBUG -> logger.debug { logMessage }
        }
    }
}
