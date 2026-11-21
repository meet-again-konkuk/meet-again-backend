package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.xroom.domain.XroomCleaner
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
    private val notificationCommandRepository: NotificationCommandRepository,
    private val commentNotificationSettingRepository: CommentNotificationSettingRepository,
    private val xroomCleaner: XroomCleaner,
    private val memberPhotoCleaner: MemberPhotoCleaner
) {

    fun clean(member: Member) {
        cleanAuth(member)
        cleanMatching(member)
        cleanPoint(member)
        cleanCommunity(member)
        cleanNotification(member)
        cleanXroom(member)
        cleanPhoto(member)
        anonymizeMember(member)
    }

    private fun cleanAuth(member: Member) {
        refreshTokenRepository.delete(member.id)
    }

    private fun cleanMatching(member: Member) {
        targetInfoCommandRepository.delete(member.id)
        matchingResultRepository.deleteByRegister(member.id)
    }

    private fun cleanPoint(member: Member) {
        memberPointRepository.delete(member.id)
    }

    private fun cleanCommunity(member: Member) {
        postLikeRepository.deleteByMember(member.id)
        commentLikeRepository.deleteByMember(member.id)
    }

    private fun cleanNotification(member: Member) {
        notificationCommandRepository.deleteByMember(member.id)
        commentNotificationSettingRepository.deleteByMember(member.id)
    }

    private fun cleanXroom(member: Member) {
        xroomCleaner.clean(member.id)
    }

    private fun cleanPhoto(member: Member) {
        memberPhotoCleaner.clean(member.id)
    }

    private fun anonymizeMember(member: Member) {
        memberCommandRepository.anonymizeAndSoftDelete(member)
    }
}
