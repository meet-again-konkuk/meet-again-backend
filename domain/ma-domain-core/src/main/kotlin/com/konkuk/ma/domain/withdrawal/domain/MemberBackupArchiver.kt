package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.withdrawal.domain.port.MemberBackupSerializer
import com.konkuk.ma.domain.withdrawal.domain.port.MemberBackupStorage
import org.springframework.stereotype.Component

@Component
class MemberBackupArchiver(
    private val collector: MemberWithdrawalBackupCollector,
    private val serializer: MemberBackupSerializer,
    private val storage: MemberBackupStorage,
) {
    fun archive(member: Member) {
        val backup = collector.collect(member)
        val content = serializer.serialize(backup)
        storage.store(BACKUP_DIRECTORY, "${member.id}$BACKUP_FILE_EXTENSION", content)
    }

    companion object {
        private const val BACKUP_DIRECTORY = "withdrawal-backup"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }
}
