package com.konkuk.ma.storage

import com.konkuk.ma.domain.withdrawal.domain.port.MemberBackupStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
@ConditionalOnProperty(name = ["backup.storage.mode"], havingValue = "s3")
class S3MemberBackupStorage(
    private val s3Client: S3Client,
    @Value("\${backup.s3.bucket}")
    private val bucket: String,
) : MemberBackupStorage {

    override fun store(directory: String, fileName: String, content: ByteArray) {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key("$directory/$fileName")
            .contentType(CONTENT_TYPE_JSON)
            .build()
        s3Client.putObject(request, RequestBody.fromBytes(content))
    }

    companion object {
        private const val CONTENT_TYPE_JSON = "application/json"
    }
}
