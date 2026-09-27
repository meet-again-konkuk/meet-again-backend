package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

@Component
@ConditionalOnProperty(name = ["file.storage.mode"], havingValue = "s3")
class S3FileUrlResolver(
    private val s3Presigner: S3Presigner,
    @Value("\${file.s3.bucket}")
    private val bucket: String,
    @Value("\${file.s3.presign-ttl-minutes:10}")
    private val presignTtlMinutes: Long,
) : FileUrlResolver {

    override fun resolve(storageKey: String): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(storageKey)
            .build()
        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(presignTtlMinutes))
            .getObjectRequest(getObjectRequest)
            .build()
        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }
}
