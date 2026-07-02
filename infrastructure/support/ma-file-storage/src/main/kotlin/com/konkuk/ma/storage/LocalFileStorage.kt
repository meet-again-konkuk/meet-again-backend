package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["file.storage.mode"], havingValue = "local", matchIfMissing = true)
class LocalFileStorage(
    @Value("\${file.upload.base-path:uploads}")
    private val basePath: String
) : FileStorage {

    override fun store(directory: String, photoFile: PhotoFile): String {
        val dir = Paths.get(basePath, directory)
        Files.createDirectories(dir)

        val storedFileName = "${UUID.randomUUID()}.${photoFile.extension.normalized}"
        val targetPath = dir.resolve(storedFileName)
        Files.write(targetPath, photoFile.content)

        return targetPath.toString()
    }

    override fun storeBytes(directory: String, fileName: String, bytes: ByteArray): String {
        val dir = Paths.get(basePath, directory)
        Files.createDirectories(dir)

        val targetPath = dir.resolve(fileName)
        Files.write(targetPath, bytes)

        return targetPath.toString()
    }

    override fun delete(filePath: String) {
        val path = Paths.get(filePath)
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }

    override fun deleteByKey(storageKey: String) {
        val path = Paths.get(basePath).resolve(storageKey)
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }
}
