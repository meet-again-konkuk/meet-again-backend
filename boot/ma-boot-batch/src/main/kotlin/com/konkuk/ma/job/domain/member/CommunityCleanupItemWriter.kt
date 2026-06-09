package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.member.domain.Member

class CommunityCleanupItemWriter(
    private val postCommandRepository: PostCommandRepository,
    private val commentCommandRepository: CommentCommandRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentLikeRepository: CommentLikeRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        val withdrawnEmail = Email.withdrawn(member.id)
        postCommandRepository.anonymizeAuthor(member.email, withdrawnEmail)
        commentCommandRepository.anonymizeAuthor(member.email, withdrawnEmail)
        postLikeRepository.deleteByMember(member.email)
        commentLikeRepository.deleteByMember(member.email)
    }
}
