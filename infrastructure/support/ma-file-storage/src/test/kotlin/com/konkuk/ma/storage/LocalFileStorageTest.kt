package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.AllowedExtension
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import java.nio.file.Files
import java.nio.file.Paths

class LocalFileStorageTest : FunSpec({

    lateinit var tempDir: java.nio.file.Path
    lateinit var storage: LocalFileStorage

    beforeEach {
        tempDir = Files.createTempDirectory("file-storage-test")
        storage = LocalFileStorage(basePath = tempDir.toString())
    }

    afterEach {
        tempDir.toFile().deleteRecursively()
    }

    context("store") {

        test("파일을 지정된 디렉토리에 저장하고 경로를 반환한다") {
            val content = "fake-image-content".toByteArray()
            val photoFile = PhotoFile(
                originalFileName = "profile.jpg",
                extension = AllowedExtension.from("profile.jpg"),
                sizeInBytes = content.size.toLong(),
                content = content,
            )

            val storedPath = storage.store("members/photos", photoFile)

            val file = Paths.get(storedPath)
            Files.exists(file) shouldBe true
            Files.readAllBytes(file).contentEquals(content) shouldBe true
            storedPath shouldEndWith ".jpg"
        }

        test("존재하지 않는 하위 디렉토리를 자동 생성한다") {
            val content = "test".toByteArray()
            val photoFile = PhotoFile(
                originalFileName = "avatar.png",
                extension = AllowedExtension.from("avatar.png"),
                sizeInBytes = content.size.toLong(),
                content = content,
            )

            val storedPath = storage.store("deep/nested/dir", photoFile)

            Files.exists(Paths.get(storedPath)) shouldBe true
        }
    }

    context("delete") {

        test("존재하는 파일을 삭제한다") {
            val content = "to-delete".toByteArray()
            val photoFile = PhotoFile(
                originalFileName = "delete-me.jpg",
                extension = AllowedExtension.from("delete-me.jpg"),
                sizeInBytes = content.size.toLong(),
                content = content,
            )
            val storedPath = storage.store("temp", photoFile)

            storage.delete(storedPath)

            Files.exists(Paths.get(storedPath)) shouldBe false
        }

        test("존재하지 않는 파일 경로로 삭제해도 예외가 발생하지 않는다") {
            storage.delete("/non/existent/path/file.jpg")
        }
    }

    context("deleteByKey") {

        test("basePath 아래 상대 storageKey의 파일을 삭제한다") {
            val storageKey = "memory/memory-photo/1/photo.jpg"
            val target = tempDir.resolve(storageKey)
            Files.createDirectories(target.parent)
            Files.write(target, "to-purge".toByteArray())
            Files.exists(target) shouldBe true

            storage.deleteByKey(storageKey)

            Files.exists(target) shouldBe false
        }

        test("존재하지 않는 storageKey로 삭제해도 예외가 발생하지 않는다") {
            storage.deleteByKey("memory/memory-photo/1/missing.jpg")
        }
    }
})
