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
        // 매칭은 memberId(비PII)를 참조하므로 익명화 불필요 — 등록자 본인 데이터만 삭제한다.
        // 내가 타인의 target으로 잡힌 매칭 결과(targetId=내 id)는 그대로 두며, 조회 시 소프트삭제된 회원이 제외돼 withdrawn으로 표시된다.
        targetInfoCommandRepository.delete(member.id)
        matchingResultRepository.deleteByRegister(member.id)
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
