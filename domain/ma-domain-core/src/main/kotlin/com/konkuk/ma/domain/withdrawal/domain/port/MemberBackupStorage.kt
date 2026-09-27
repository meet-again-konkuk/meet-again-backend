package com.konkuk.ma.domain.withdrawal.domain.port

interface MemberBackupStorage {
    fun store(directory: String, fileName: String, content: ByteArray)
}
