package com.konkuk.ma.storage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class LocalMemberBackupStorageTest : FunSpec({

    lateinit var tempDir: java.nio.file.Path
    lateinit var storage: LocalMemberBackupStorage

    beforeEach {
        tempDir = Files.createTempDirectory("member-backup-test")
        storage = LocalMemberBackupStorage(basePath = tempDir.toString())
    }

    afterEach {
        tempDir.toFile().deleteRecursively()
    }

    context("store") {

        test("백업 파일을 지정한 디렉토리에 저장한다") {
            val content = """{"id":1}""".toByteArray()

            storage.store("withdrawal-backup", "1.json", content)

            val file = tempDir.resolve("withdrawal-backup").resolve("1.json")
            Files.exists(file) shouldBe true
            Files.readAllBytes(file).contentEquals(content) shouldBe true
        }

        test("존재하지 않는 하위 디렉토리를 자동 생성한다") {
            val content = "backup".toByteArray()

            storage.store("withdrawal-backup/2026-06-17", "2.json", content)

            Files.exists(tempDir.resolve("withdrawal-backup/2026-06-17").resolve("2.json")) shouldBe true
        }

        test("같은 키로 다시 저장하면 덮어쓴다 (재시도 멱등성)") {
            storage.store("withdrawal-backup", "3.json", "old".toByteArray())
            storage.store("withdrawal-backup", "3.json", "new".toByteArray())

            val file = tempDir.resolve("withdrawal-backup").resolve("3.json")
            String(Files.readAllBytes(file)) shouldBe "new"
        }
    }
})
