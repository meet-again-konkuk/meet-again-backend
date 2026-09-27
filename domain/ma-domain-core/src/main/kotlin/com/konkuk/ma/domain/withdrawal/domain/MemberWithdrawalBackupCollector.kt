package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.domain.support.domain.port.InquiryQueryRepository
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.Xrooms
import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.memory.Memories
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.port.MediaQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Component

@Component
class MemberWithdrawalBackupCollector(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val matchingResultRepository: MatchingResultRepository,
    private val memberPointRepository: MemberPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val inquiryQueryRepository: InquiryQueryRepository,
    private val xroomQueryRepository: XroomQueryRepository,
    private val memoryQueryRepository: MemoryQueryRepository,
    private val mediaQueryRepository: MediaQueryRepository,
    private val memberPhotoRepository: MemberPhotoRepository,
) {
    fun collect(member: Member): MemberWithdrawalBackup {
        val xrooms = xroomQueryRepository.find(member.id)
        val memories = collectMemories(xrooms)
        val medias = collectMedias(memories)
        return MemberWithdrawalBackup(
            member = MemberBackupView.from(member),
            targetInfos = targetInfoQueryRepository.find(member.id),
            registeredMatchingResults = matchingResultRepository.find(member.id),
            claimedMatchingResults = matchingResultRepository.findClaimedByTarget(member.id),
            pointBalance = memberPointRepository.findOneOrInitial(member.id),
            pointHistories = pointHistoryRepository.find(member.id),
            posts = postQueryRepository.findByAuthor(member.id),
            comments = commentQueryRepository.findByAuthor(member.id),
            postLikes = postLikeRepository.find(member.id),
            commentLikes = commentLikeRepository.find(member.id),
            inquiries = inquiryQueryRepository.find(member.id),
            xrooms = xrooms,
            memories = memories,
            medias = medias,
            photo = memberPhotoRepository.findOne(member.id),
        )
    }

    private fun collectMemories(xrooms: List<Xroom>): List<Memory> {
        return memoryQueryRepository.find(Xrooms(xrooms).extractIds())
    }

    private fun collectMedias(memories: List<Memory>): List<Media> {
        return mediaQueryRepository.findActiveByMemories(Memories(memories).extractIds())
    }
}
