package com.konkuk.ma.member.domain.port

interface PasswordEncryptor {
    fun encode(rawPassword: String): String
    fun matches(rawPassword: String, encodedPassword: String): Boolean
} 