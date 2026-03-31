package com.konkuk.ma.domain.common.port

interface IdObfuscator {
    fun encode(id: Long): String
    fun decode(encoded: String): Long
}
