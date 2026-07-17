package com.konkuk.ma.storage

import com.konkuk.ma.config.S3StorageConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.MapPropertySource
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

// 문자열 SpEL 전환 조건과 S3Client 실생성(httpclient5/httpcore5 버전 오버라이드)은 단위 테스트로 잡히지 않아 실제 컨텍스트로 고정
class MemberBackupStorageWiringTest : FunSpec({

    fun contextWith(properties: Map<String, Any>): AnnotationConfigApplicationContext {
        val context = AnnotationConfigApplicationContext()
        context.environment.propertySources.addFirst(MapPropertySource("smoke", properties))
        context.register(
            PropertySourcesPlaceholderConfigurer::class.java,
            S3StorageConfig::class.java,
            LocalMemberBackupStorage::class.java,
            S3MemberBackupStorage::class.java,
        )
        context.refresh()
        return context
    }

    test("mode 미설정이면 Local만 활성, S3 빈 전부 부재") {
        contextWith(emptyMap()).use { context ->
            context.containsBean("localMemberBackupStorage").shouldBeTrue()
            context.containsBean("s3MemberBackupStorage").shouldBeFalse()
            context.getBeansOfType(S3Client::class.java).size shouldBe 0
        }
    }

    test("backup.storage.mode=local이면 Local만 활성") {
        contextWith(mapOf("backup.storage.mode" to "local")).use { context ->
            context.containsBean("localMemberBackupStorage").shouldBeTrue()
            context.containsBean("s3MemberBackupStorage").shouldBeFalse()
        }
    }

    test("backup.storage.mode=s3면 S3 어댑터+S3Client 활성, Local·Presigner 부재") {
        contextWith(
            mapOf(
                "backup.storage.mode" to "s3",
                "backup.s3.bucket" to "backup-bucket",
                "file.s3.region" to "ap-northeast-2",
            ),
        ).use { context ->
            context.containsBean("s3MemberBackupStorage").shouldBeTrue()
            context.containsBean("localMemberBackupStorage").shouldBeFalse()
            context.getBeansOfType(S3Client::class.java).size shouldBe 1
            context.getBeansOfType(S3Presigner::class.java).size shouldBe 0
        }
    }

    test("file.storage.mode=s3면 S3Client·Presigner 활성, 백업은 Local 유지") {
        contextWith(
            mapOf(
                "file.storage.mode" to "s3",
                "file.s3.region" to "ap-northeast-2",
            ),
        ).use { context ->
            context.getBeansOfType(S3Client::class.java).size shouldBe 1
            context.getBeansOfType(S3Presigner::class.java).size shouldBe 1
            context.containsBean("localMemberBackupStorage").shouldBeTrue()
            context.containsBean("s3MemberBackupStorage").shouldBeFalse()
        }
    }
})
