package com.konkuk.ma.domain.auth.domain.port

interface PasswordEncryptor {
    fun encode(rawPassword: String): String
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
