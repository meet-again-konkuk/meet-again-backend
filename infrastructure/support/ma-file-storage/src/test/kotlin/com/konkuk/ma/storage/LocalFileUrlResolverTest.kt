package com.konkuk.ma.storage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LocalFileUrlResolverTest : FunSpec({

    context("resolve") {

        test("기본 prefix(/files)와 storageKey를 결합해 URL을 만든다") {
            // Given
            val resolver = LocalFileUrlResolver(urlPrefix = "/files")
            val storageKey = "memory/memory-photo/1/photo.jpg"

            // When
            val url = resolver.resolve(storageKey)

            // Then
            url shouldBe "/files/$storageKey"
        }

        test("커스텀 prefix와 storageKey를 결합해 URL을 만든다") {
            // Given
            val urlPrefix = "https://cdn.example.com/assets"
            val resolver = LocalFileUrlResolver(urlPrefix = urlPrefix)
            val storageKey = "memory/thumbnail/1/thumb_photo.jpg"

            // When
            val url = resolver.resolve(storageKey)

            // Then
            url shouldBe "$urlPrefix/$storageKey"
        }
    }
})
