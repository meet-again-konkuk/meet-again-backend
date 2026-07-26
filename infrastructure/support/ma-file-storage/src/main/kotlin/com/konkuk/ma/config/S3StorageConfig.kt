package com.konkuk.ma.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@ConditionalOnExpression(
    "'\${file.storage.mode:local}' == 's3' or '\${backup.storage.mode:local}' == 's3'",
)
class S3StorageConfig(
    // 사진(S3FileStorage)·백업(S3MemberBackupStorage)이 공유하는 리전 — 백업만 s3 모드여도 필수
    @Value("\${file.s3.region}")
    private val region: String,
) {
    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.of(region))
            .build()
    }

    @Bean
    @ConditionalOnProperty(name = ["file.storage.mode"], havingValue = "s3")
    fun s3Presigner(): S3Presigner {
        return S3Presigner.builder()
            .region(Region.of(region))
            .build()
    }
}
