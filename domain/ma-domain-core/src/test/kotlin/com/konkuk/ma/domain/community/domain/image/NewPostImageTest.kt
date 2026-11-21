package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewPostImageTest : FunSpec({

    context("of") {

        test("PhotoFile 의 메타와 저장 키를 보존해 NewPostImage 를 만든다") {
            val postId = 42L
            val photoFile = PhotoFileFixture.create(originalFileName = "sunset.png")
            val storageKey = "community/post-image/$postId/uuid.png"
            val thumbnailKey = "community/thumbnail/$postId/thumb_sunset.png"

            val newPostImage = NewPostImage.of(postId, photoFile, storageKey, thumbnailKey)

            newPostImage.postId shouldBe postId
            newPostImage.storageKey shouldBe storageKey
            newPostImage.thumbnailKey shouldBe thumbnailKey
            newPostImage.originalFilename shouldBe photoFile.originalFileName
            newPostImage.mimeType shouldBe photoFile.mimeType
            newPostImage.fileSize shouldBe photoFile.sizeInBytes
        }

        test("썸네일 키가 없으면 thumbnailKey 가 null 인 NewPostImage 를 만든다") {
            val photoFile = PhotoFileFixture.create()

            val newPostImage = NewPostImage.of(1L, photoFile, "community/post-image/1/uuid.jpg", null)

            newPostImage.thumbnailKey shouldBe null
        }
    }

    context("toPostImage") {

        test("부여받은 id 와 함께 모든 필드를 그대로 옮긴 PostImage 를 만든다") {
            val newPostImage = NewPostImage.of(
                postId = 5L,
                photoFile = PhotoFileFixture.create(originalFileName = "photo.jpg"),
                storageKey = "community/post-image/5/uuid.jpg",
                thumbnailKey = "community/thumbnail/5/thumb_photo.jpg",
            )
            val savedId = 100L

            val postImage = newPostImage.toPostImage(savedId)

            postImage.id shouldBe savedId
            postImage.postId shouldBe newPostImage.postId
            postImage.storageKey shouldBe newPostImage.storageKey
            postImage.originalFilename shouldBe newPostImage.originalFilename
            postImage.mimeType shouldBe newPostImage.mimeType
            postImage.fileSize shouldBe newPostImage.fileSize
            postImage.thumbnailKey shouldBe newPostImage.thumbnailKey
        }
    }
})
