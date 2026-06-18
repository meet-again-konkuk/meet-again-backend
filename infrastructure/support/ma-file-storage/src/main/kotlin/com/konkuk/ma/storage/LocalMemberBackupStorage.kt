package com.konkuk.ma.storage

import com.konkuk.ma.domain.withdrawal.domain.port.MemberBackupStorage
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class LocalMemberBackupStorage(
    @Value("\${backup.local.base-path:backups}")
    private val basePath: String,
) : MemberBackupStorage {

    override fun store(directory: String, fileName: String, content: ByteArray) {
        val dir = Paths.get(basePath, directory)
        Files.createDirectories(dir)
        Files.write(dir.resolve(fileName), content)
    }
}
