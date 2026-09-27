package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
@ConditionalOnProperty(name = ["file.storage.mode"], havingValue = "s3")
class S3FileStorage(
    private val s3Client: S3Client,
    @Value("\${file.s3.bucket}")
    private val bucket: String,
) : FileStorage {

    override fun store(directory: String, photoFile: PhotoFile): String {
        val key = "$directory/${UUID.randomUUID()}.${photoFile.extension.normalized}"
        putObject(key, photoFile.content, photoFile.mimeType)
        return key
    }

    override fun storeBytes(directory: String, fileName: String, bytes: ByteArray): String {
        val key = "$directory/$fileName"
        putObject(key, bytes, null)
        return key
    }

    override fun deleteByKey(storageKey: String) = deleteObject(storageKey)

    private fun putObject(key: String, bytes: ByteArray, mimeType: String?) {
        val builder = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
        mimeType?.let { builder.contentType(it) }
        s3Client.putObject(builder.build(), RequestBody.fromBytes(bytes))
    }

    private fun deleteObject(key: String) {
        val request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()
        s3Client.deleteObject(request)
    }
}
