package com.konkuk.ma.domain.common.domain.id.port

import com.konkuk.ma.domain.common.domain.id.ObfuscationType

interface IdObfuscator {
    fun encode(type: ObfuscationType, id: Long): String
    fun decode(type: ObfuscationType, encoded: String): Long
}
