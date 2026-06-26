package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Xroom(
    val id: Long,
    val ownerId: Long,
    val targetInfoId: Long,
    val title: XroomTitle,
    val template: XroomTemplate,
    val finalMessage: FinalMessage?,
    val createdDate: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    val titleValue: String get() = title.value
    val templateValue: String get() = template.wireValue
    val finalMessageValue: String? get() = finalMessage?.value

    fun isOwnedBy(memberId: Long): Boolean = ownerId == memberId

    fun validateOwnership(memberId: Long) {
        if (ownerId != memberId) {
            throw AccessDeniedException(EntityType.XROOM, ownerId.toString(), memberId.toString())
        }
    }

    fun validateRecipient(receivedTargetInfoIds: Set<Long>) {
        if (targetInfoId !in receivedTargetInfoIds) {
            throw AccessDeniedException(EntityType.XROOM, id.toString(), targetInfoId.toString())
        }
    }

    fun updateFinalMessage(finalMessage: String?): Xroom {
        return Xroom(
            id = id,
            ownerId = ownerId,
            targetInfoId = targetInfoId,
            title = title,
            template = template,
            finalMessage = FinalMessage.of(finalMessage),
            createdDate = createdDate,
            updatedAt = updatedAt,
        )
    }
}
