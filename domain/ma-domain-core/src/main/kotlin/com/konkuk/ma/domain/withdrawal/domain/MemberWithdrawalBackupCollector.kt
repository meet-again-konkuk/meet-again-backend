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
    private val memberPhotoRepository: MemberPhotoRepository,
) {
    fun collect(member: Member): MemberWithdrawalBackup {
        val email = member.email
        return MemberWithdrawalBackup(
            member = MemberBackupView.from(member),
            targetInfos = targetInfoQueryRepository.find(email),
            registeredMatchingResults = matchingResultRepository.find(email),
            claimedMatchingResults = matchingResultRepository.findClaimedByTarget(email),
            pointBalance = memberPointRepository.findOneOrInitial(email),
            pointHistories = pointHistoryRepository.find(email),
            posts = postQueryRepository.find(email),
            comments = commentQueryRepository.find(email),
            postLikes = postLikeRepository.find(email),
            commentLikes = commentLikeRepository.find(email),
            inquiries = inquiryQueryRepository.find(email),
            xrooms = xroomQueryRepository.find(email),
            photo = memberPhotoRepository.findOne(email),
        )
    }
}
