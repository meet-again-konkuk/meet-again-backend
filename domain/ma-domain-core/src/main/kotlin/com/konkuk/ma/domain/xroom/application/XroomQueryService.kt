package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.xroom.domain.MyXrooms
import com.konkuk.ma.domain.xroom.domain.ReceivedXrooms
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomDetail
import com.konkuk.ma.domain.xroom.domain.Xrooms
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class XroomQueryService(
    private val xroomQueryRepository: XroomQueryRepository,
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val matchingResultRepository: MatchingResultRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun findMine(memberId: Long): MyXrooms {
        val xrooms = Xrooms(xroomQueryRepository.find(memberId))
        val targetInfos = TargetInfos(targetInfoQueryRepository.find(memberId))
        return xrooms.toMine(targetInfos)
    }

    fun findReceived(memberId: Long): ReceivedXrooms {
        val matchingResults = MatchingResults(matchingResultRepository.findClaimedByTarget(memberId))
        val targetInfoIds = matchingResults.extractTargetInfoIds()

        val xrooms = Xrooms(xroomQueryRepository.findByTargetInfoIds(targetInfoIds))
        val senders = Members(memberQueryRepository.findByIds(xrooms.extractOwnerIds()))

        return xrooms.toReceived(senders)
    }

    fun findDetail(xroomId: Long, memberId: Long): XroomDetail {
        val xroom = xroomQueryRepository.findOne(xroomId)
        validateAccessible(xroom, memberId)

        val targetInfo = targetInfoQueryRepository.findOne(xroom.targetInfoId)
        return XroomDetail(xroom = xroom, recipientName = targetInfo.targetName)
    }

    private fun validateAccessible(xroom: Xroom, memberId: Long) {
        if (xroom.isOwnedBy(memberId)) return

        val receivedTargetInfoIds =
            MatchingResults(matchingResultRepository.findClaimedByTarget(memberId)).extractTargetInfoIds()
        xroom.validateRecipient(receivedTargetInfoIds)
    }
}
