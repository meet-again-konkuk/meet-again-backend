package com.konkuk.ma.storage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.net.URI
import java.time.Duration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest

class S3FileUrlResolverTest : FunSpec({

    val s3Presigner = mockk<S3Presigner>()
    val bucket = "test-bucket"

    context("resolve") {

        test("bucket·storageKey·TTL(분)로 presigned GET URL을 요청하고 URL 문자열을 반환한다") {
            // Given
            val presignTtlMinutes = 10L
            val resolver = S3FileUrlResolver(s3Presigner, bucket, presignTtlMinutes)
            val storageKey = "memory/memory-photo/1/photo.jpg"
            val expectedUrl = URI.create("https://$bucket.s3.amazonaws.com/$storageKey?signature=abc123").toURL()
            val presigned = mockk<PresignedGetObjectRequest>()
            every { presigned.url() } returns expectedUrl
            val requestSlot = slot<GetObjectPresignRequest>()
            every { s3Presigner.presignGetObject(capture(requestSlot)) } returns presigned

            // When
            val result = resolver.resolve(storageKey)

            // Then
            result shouldBe expectedUrl.toString()
            requestSlot.captured.signatureDuration() shouldBe Duration.ofMinutes(presignTtlMinutes)
            val getObjectRequest: GetObjectRequest = requestSlot.captured.getObjectRequest()
            getObjectRequest.bucket() shouldBe bucket
            getObjectRequest.key() shouldBe storageKey
        }

        test("설정된 TTL(분)이 signatureDuration에 그대로 반영된다") {
            // Given
            val presignTtlMinutes = 30L
            val resolver = S3FileUrlResolver(s3Presigner, bucket, presignTtlMinutes)
            val storageKey = "memory/thumbnail/1/thumb_photo.jpg"
            val presigned = mockk<PresignedGetObjectRequest>()
            every { presigned.url() } returns URI.create("https://$bucket.s3.amazonaws.com/$storageKey").toURL()
            val requestSlot = slot<GetObjectPresignRequest>()
            every { s3Presigner.presignGetObject(capture(requestSlot)) } returns presigned

            // When
            resolver.resolve(storageKey)

            // Then
            requestSlot.captured.signatureDuration() shouldBe Duration.ofMinutes(presignTtlMinutes)
        }
    }
})
