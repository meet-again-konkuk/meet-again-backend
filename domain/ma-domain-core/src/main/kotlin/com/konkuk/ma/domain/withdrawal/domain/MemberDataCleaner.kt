package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import org.springframework.stereotype.Component

@Component
class MemberDataCleaner(
    private val memberCommandRepository: MemberCommandRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val targetInfoCommandRepository: TargetInfoCommandRepository,
    private val matchingResultRepository: MatchingResultRepository,
    private val memberPointRepository: MemberPointRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val xroomCommandRepository: XroomCommandRepository,
    private val memberPhotoCleaner: MemberPhotoCleaner
) {

    fun clean(member: Member) {
        cleanAuth(member)
        cleanMatching(member)
        cleanPoint(member)
        cleanCommunity(member)
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
        // 포인트는 ownerId(비PII)를 참조하므로 익명화 불필요 — 잔액만 삭제하고, 결제 이력은 ownerId로 보존한다.
        memberPointRepository.delete(member.id)
    }

    private fun cleanCommunity(member: Member) {
        // 글·댓글은 authorId(비PII) 참조라 익명화 불필요(소프트삭제 회원은 조회 시 "알 수 없음"); 좋아요만 삭제.
        postLikeRepository.deleteByMember(member.id)
        commentLikeRepository.deleteByMember(member.id)
    }

    private fun cleanXroom(member: Member) {
        xroomCommandRepository.delete(member.id)
    }

    private fun cleanPhoto(member: Member) {
        memberPhotoCleaner.clean(member.id)
    }

    private fun anonymizeMember(member: Member) {
        memberCommandRepository.anonymizeAndSoftDelete(member)
    }
}
