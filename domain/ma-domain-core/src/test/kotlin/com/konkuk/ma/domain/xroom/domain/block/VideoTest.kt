package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockDomainFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class VideoTest : FunSpec({

    context("validateBelongsTo") {

        test("video의 blockId와 expectedBlockId가 같으면 예외 없이 통과한다") {
            val video = XroomBlockDomainFixture.video(blockId = 100L)

            video.validateBelongsTo(video.blockId)
        }

        test("video의 blockId와 expectedBlockId가 다르면 AccessDeniedException이 발생한다") {
            val video = XroomBlockDomainFixture.video(blockId = 100L)

            shouldThrow<AccessDeniedException> {
                video.validateBelongsTo(999L)
            }
        }
    }
})
