package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockDomainFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class PhotoTest : FunSpec({

    context("validateBelongsTo") {

        test("photo의 blockId와 expectedBlockId가 같으면 예외 없이 통과한다") {
            val photo = XroomBlockDomainFixture.photo(blockId = 100L)

            photo.validateBelongsTo(photo.blockId)
        }

        test("photo의 blockId와 expectedBlockId가 다르면 AccessDeniedException이 발생한다") {
            val photo = XroomBlockDomainFixture.photo(blockId = 100L)

            shouldThrow<AccessDeniedException> {
                photo.validateBelongsTo(999L)
            }
        }
    }
})
