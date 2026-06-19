package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.domain.support.domain.port.InquiryCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import org.springframework.stereotype.Component

@Component
class MemberDataCleaner(
    private val memberCommandRepository: MemberCommandRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val targetInfoCommandRepository: TargetInfoCommandRepository,
    private val matchingResultRepository: MatchingResultRepository,
    private val memberPointRepository: MemberPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val postCommandRepository: PostCommandRepository,
    private val commentCommandRepository: CommentCommandRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val inquiryCommandRepository: InquiryCommandRepository,
    private val xroomCommandRepository: XroomCommandRepository,
    private val memberPhotoCleaner: MemberPhotoCleaner
) {

    fun clean(member: Member) {
        cleanAuth(member)
        cleanMatching(member)
        cleanPoint(member)
        cleanCommunity(member)
        cleanSupport(member)
        cleanXroom(member)
        cleanPhoto(member)
        anonymizeMember(member)
    }

    private fun cleanAuth(member: Member) {
        refreshTokenRepository.delete(member.id)
    }

    private fun cleanMatching(member: Member) {
        targetInfoCommandRepository.delete(member.email)
        matchingResultRepository.deleteByRegister(member.email)
        matchingResultRepository.anonymizeTarget(member.email, member.withdrawnEmail())
    }

    private fun cleanPoint(member: Member) {
        memberPointRepository.delete(member.email)
        pointHistoryRepository.anonymizeOwner(member.email, member.withdrawnEmail())
    }

    private fun cleanCommunity(member: Member) {
        val withdrawnEmail = member.withdrawnEmail()
        postCommandRepository.anonymizeAuthor(member.email, withdrawnEmail)
        commentCommandRepository.anonymizeAuthor(member.email, withdrawnEmail)
        postLikeRepository.deleteByMember(member.email)
        commentLikeRepository.deleteByMember(member.email)
    }

    private fun cleanSupport(member: Member) {
        inquiryCommandRepository.anonymizeAuthor(member.email, member.withdrawnEmail())
    }

    private fun cleanXroom(member: Member) {
        xroomCommandRepository.delete(member.email)
    }

    private fun cleanPhoto(member: Member) {
        memberPhotoCleaner.clean(member.email)
    }

    private fun anonymizeMember(member: Member) {
        memberCommandRepository.anonymizeAndSoftDelete(member)
    }
}
