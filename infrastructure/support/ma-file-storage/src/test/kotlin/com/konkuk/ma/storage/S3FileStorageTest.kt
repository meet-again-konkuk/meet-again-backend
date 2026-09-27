package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class S3FileStorageTest : FunSpec({

    val s3Client = mockk<S3Client>()
    val bucket = "test-bucket"
    val storage = S3FileStorage(s3Client, bucket)

    context("store") {

        test("directory/UUID.확장자 키로 mimeType과 함께 putObject를 호출하고 키를 반환한다") {
            // Given
            val directory = "memory/memory-photo/1"
            val photoFile = PhotoFileFixture.create(originalFileName = "photo.jpg")
            val requestSlot = slot<PutObjectRequest>()
            every {
                s3Client.putObject(capture(requestSlot), any<RequestBody>())
            } returns PutObjectResponse.builder().build()

            // When
            val storedKey = storage.store(directory, photoFile)

            // Then
            val captured = requestSlot.captured
            captured.bucket() shouldBe bucket
            captured.key() shouldBe storedKey
            captured.contentType() shouldBe photoFile.mimeType
            storedKey shouldMatch Regex("$directory/[0-9a-f-]{36}\\.${photoFile.extension.normalized}")
        }
    }

    context("storeBytes") {

        test("directory/fileName 키로 contentType 없이 putObject를 호출하고 키를 반환한다") {
            // Given
            val directory = "memory/thumbnail/1"
            val fileName = "thumb_photo.jpg"
            val bytes = ByteArray(512) { 7 }
            val requestSlot = slot<PutObjectRequest>()
            every {
                s3Client.putObject(capture(requestSlot), any<RequestBody>())
            } returns PutObjectResponse.builder().build()

            // When
            val storedKey = storage.storeBytes(directory, fileName, bytes)

            // Then
            val captured = requestSlot.captured
            storedKey shouldBe "$directory/$fileName"
            captured.bucket() shouldBe bucket
            captured.key() shouldBe storedKey
            captured.contentType().shouldBeNull()
        }
    }

    context("deleteByKey") {

        test("주어진 storageKey를 키로 deleteObject를 호출한다") {
            // Given
            val storageKey = "memory/thumbnail/1/thumb_photo.jpg"
            val requestSlot = slot<DeleteObjectRequest>()
            every {
                s3Client.deleteObject(capture(requestSlot))
            } returns DeleteObjectResponse.builder().build()

            // When
            storage.deleteByKey(storageKey)

            // Then
            val captured = requestSlot.captured
            captured.bucket() shouldBe bucket
            captured.key() shouldBe storageKey
        }
    }
})
