package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.history.PointHistory
import com.konkuk.ma.domain.support.domain.Inquiry
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.memory.Memory

/**
 * 탈퇴 정리(삭제·익명화) 직전에 보존하는 회원 전체 스냅샷.
 * 분쟁 대응 근거 자료로 저장소에 업로드된다.
 */
class MemberWithdrawalBackup(
    val member: MemberBackupView,
    val targetInfos: List<TargetInfo>,
    val registeredMatchingResults: List<MatchingResult>,
    val claimedMatchingResults: List<MatchingResult>,
    val pointBalance: MemberPoint,
    val pointHistories: List<PointHistory>,
    val posts: List<Post>,
    val comments: List<Comment>,
    val postLikes: List<PostLike>,
    val commentLikes: List<CommentLike>,
    val inquiries: List<Inquiry>,
    val xrooms: List<Xroom>,
    val memories: List<Memory>,
    val medias: List<Media>,
    val photo: MemberPhoto?,
)
