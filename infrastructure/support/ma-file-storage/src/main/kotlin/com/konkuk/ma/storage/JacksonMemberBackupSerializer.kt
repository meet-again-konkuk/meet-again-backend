package com.konkuk.ma.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.konkuk.ma.domain.withdrawal.domain.MemberWithdrawalBackup
import com.konkuk.ma.domain.withdrawal.domain.port.MemberBackupSerializer
import org.springframework.stereotype.Component

@Component
class JacksonMemberBackupSerializer : MemberBackupSerializer {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

    override fun serialize(backup: MemberWithdrawalBackup): ByteArray {
        return objectMapper.writeValueAsBytes(backup)
    }
}
