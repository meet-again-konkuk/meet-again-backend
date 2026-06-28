package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.xroom.domain.MyXrooms
import com.konkuk.ma.domain.xroom.domain.ReceivedXrooms
import com.konkuk.ma.domain.xroom.domain.XroomDetail
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.Xrooms
import com.konkuk.ma.domain.xroom.domain.memory.Memories
import com.konkuk.ma.domain.xroom.domain.memory.MemoryCounts
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
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
    private val memoryQueryRepository: MemoryQueryRepository,
    private val xroomValidator: XroomValidator,
) {
    fun findMine(memberId: Long): MyXrooms {
        val xrooms = Xrooms(xroomQueryRepository.find(memberId))
        val targetInfos = TargetInfos(targetInfoQueryRepository.find(memberId))
        val memoryCounts = MemoryCounts(memoryQueryRepository.count(xrooms.extractIds()))
        return xrooms.toMine(targetInfos, memoryCounts)
    }

    fun findReceived(memberId: Long): ReceivedXrooms {
        val matchingResults = MatchingResults(matchingResultRepository.findClaimedByTarget(memberId))
        val targetInfoIds = matchingResults.extractTargetInfoIds()

        val xrooms = Xrooms(xroomQueryRepository.findByTargetInfoIds(targetInfoIds))
        val senders = Members(memberQueryRepository.findByIds(xrooms.extractOwnerIds()))
        val memoryCounts = MemoryCounts(memoryQueryRepository.count(xrooms.extractIds()))

        return xrooms.toReceived(senders, memoryCounts)
    }

    fun findDetail(xroomId: Long, memberId: Long): XroomDetail {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroomValidator.validateAccessible(xroom, memberId)

        val targetInfo = targetInfoQueryRepository.findOne(xroom.targetInfoId)
        val memories = Memories(memoryQueryRepository.find(xroomId))
        return XroomDetail(xroom = xroom, recipientName = targetInfo.targetName, memoriesCollection = memories)
    }
}
