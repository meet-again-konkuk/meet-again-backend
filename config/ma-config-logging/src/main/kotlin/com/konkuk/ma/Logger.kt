package com.konkuk.ma

import io.github.oshai.kotlinlogging.KotlinLogging

interface AppLogger {
    fun info(message: () -> String)
    fun debug(message: () -> String)
    fun warn(message: () -> String)
    fun warn(t: Throwable?, message: () -> String)
    fun error(t: Throwable? = null, message: () -> String)
}

private class KotlinLoggingLogger : AppLogger {
    private val delegate = KotlinLogging.logger { }
    override fun info(message: () -> String) = delegate.info(message)
    override fun warn(t: Throwable?, message: () -> String) = delegate.warn(t) { message }
    override fun warn(message: () -> String) = delegate.warn(message)
    override fun debug(message: () -> String) = delegate.debug(message)
    override fun error(t: Throwable?, message: () -> String) =
        if (t != null) delegate.error(t, message) else delegate.error(message)
}

val logger: AppLogger = KotlinLoggingLogger()
