package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.support.id.EncryptId

class TargetInfoResponse(
    @EncryptId(ObfuscationType.TARGET_INFO)
    val targetInfoId: Long,
    val targetName: String,
    val targetGender: String,
    val middleNumber: String?,
    val lastNumber: String?,
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val region: String?,
) {
    companion object {
        fun from(targetInfo: TargetInfo): TargetInfoResponse {
            return TargetInfoResponse(
                targetInfoId = targetInfo.targetInfoId,
                targetName = targetInfo.targetName,
                targetGender = targetInfo.targetGender.name,
                middleNumber = targetInfo.middleNumber?.value,
                lastNumber = targetInfo.lastNumber?.value,
                year = targetInfo.year?.value,
                month = targetInfo.month?.value,
                day = targetInfo.day?.value,
                region = targetInfo.region?.name,
            )
        }
    }
}
