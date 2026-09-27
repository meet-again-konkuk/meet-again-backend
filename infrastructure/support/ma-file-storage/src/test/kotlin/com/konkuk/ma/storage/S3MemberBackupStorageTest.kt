package com.konkuk.ma.storage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class S3MemberBackupStorageTest : FunSpec({

    val s3Client = mockk<S3Client>()
    val bucket = "test-backup-bucket"
    val storage = S3MemberBackupStorage(s3Client, bucket)

    context("store") {

        test("directory/fileName 키와 application/json contentType으로 putObject를 호출한다") {
            // Given
            val directory = "withdrawal-backup"
            val fileName = "1.json"
            val content = """{"memberId":1}""".toByteArray()
            val requestSlot = slot<PutObjectRequest>()
            val bodySlot = slot<RequestBody>()
            every {
                s3Client.putObject(capture(requestSlot), capture(bodySlot))
            } returns PutObjectResponse.builder().build()

            // When
            storage.store(directory, fileName, content)

            // Then
            val captured = requestSlot.captured
            captured.bucket() shouldBe bucket
            captured.key() shouldBe "$directory/$fileName"
            captured.contentType() shouldBe "application/json"
            bodySlot.captured.contentStreamProvider().newStream().readBytes() shouldBe content
        }

        test("같은 키로 다시 저장하면 예외 없이 putObject를 재호출한다") {
            // Given
            val retryS3Client = mockk<S3Client>()
            val retryStorage = S3MemberBackupStorage(retryS3Client, bucket)
            val directory = "withdrawal-backup"
            val fileName = "2.json"
            every {
                retryS3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())
            } returns PutObjectResponse.builder().build()

            // When
            retryStorage.store(directory, fileName, """{"memberId":2}""".toByteArray())
            retryStorage.store(directory, fileName, """{"memberId":2,"retried":true}""".toByteArray())

            // Then
            verify(exactly = 2) {
                retryS3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())
            }
        }
    }
})
