package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner
import com.konkuk.ma.domain.member.domain.policy.WithdrawalPolicy
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.domain.support.domain.port.InquiryCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.job.common.AbstractJobConfig
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

@Configuration
class MemberWithdrawalCompleteJobConfig(
    private val memberQueryRepository: MemberQueryRepository,
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
) : AbstractJobConfig() {

    @Bean
    fun memberWithdrawalCompleteJob(
        @Qualifier("authCleanupStep") authCleanupStep: Step,
        @Qualifier("matchingCleanupStep") matchingCleanupStep: Step,
        @Qualifier("pointCleanupStep") pointCleanupStep: Step,
        @Qualifier("communityCleanupStep") communityCleanupStep: Step,
        @Qualifier("supportCleanupStep") supportCleanupStep: Step,
        @Qualifier("xroomCleanupStep") xroomCleanupStep: Step,
        @Qualifier("memberPhotoCleanupStep") memberPhotoCleanupStep: Step,
        @Qualifier("memberAnonymizeStep") memberAnonymizeStep: Step
    ): Job {
        return JobBuilder("memberWithdrawalCompleteJob", jobRepository)
            .start(authCleanupStep)
            .next(matchingCleanupStep)
            .next(pointCleanupStep)
            .next(communityCleanupStep)
            .next(supportCleanupStep)
            .next(xroomCleanupStep)
            .next(memberPhotoCleanupStep)
            .next(memberAnonymizeStep)
            .build()
    }

    @Bean
    @JobScope
    fun jobStartTime(): JobStartTime = JobStartTime(LocalDateTime.now())

    @Bean
    fun authCleanupStep(@Qualifier("authCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep("authCleanupStep", reader, AuthCleanupItemWriter(refreshTokenRepository))

    @Bean
    fun matchingCleanupStep(@Qualifier("matchingCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "matchingCleanupStep",
            reader,
            MatchingCleanupItemWriter(targetInfoCommandRepository, matchingResultRepository)
        )

    @Bean
    fun pointCleanupStep(@Qualifier("pointCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "pointCleanupStep",
            reader,
            PointCleanupItemWriter(memberPointRepository, pointHistoryRepository)
        )

    @Bean
    fun communityCleanupStep(@Qualifier("communityCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "communityCleanupStep",
            reader,
            CommunityCleanupItemWriter(postCommandRepository, commentCommandRepository, postLikeRepository, commentLikeRepository)
        )

    @Bean
    fun supportCleanupStep(@Qualifier("supportCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "supportCleanupStep",
            reader,
            SupportCleanupItemWriter(inquiryCommandRepository)
        )

    @Bean
    fun xroomCleanupStep(@Qualifier("xroomCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "xroomCleanupStep",
            reader,
            XroomCleanupItemWriter(xroomCommandRepository)
        )

    @Bean
    fun memberPhotoCleanupStep(@Qualifier("memberPhotoCleanupReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "memberPhotoCleanupStep",
            reader,
            MemberPhotoCleanupItemWriter(memberPhotoCleaner)
        )

    @Bean
    fun memberAnonymizeStep(@Qualifier("memberAnonymizeReader") reader: ExpiredWithdrawalMemberItemReader): Step =
        buildCleanupStep(
            "memberAnonymizeStep",
            reader,
            MemberAnonymizeItemWriter(memberCommandRepository)
        )

    @Bean @StepScope fun authCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun matchingCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun pointCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun communityCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun supportCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun xroomCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun memberPhotoCleanupReader(jobStartTime: JobStartTime) = newReader(jobStartTime)
    @Bean @StepScope fun memberAnonymizeReader(jobStartTime: JobStartTime) = newReader(jobStartTime)

    private fun newReader(jobStartTime: JobStartTime): ExpiredWithdrawalMemberItemReader {
        val expiredBefore = jobStartTime.value.minusDays(WithdrawalPolicy.GRACE_PERIOD_DAYS)
        return ExpiredWithdrawalMemberItemReader(memberQueryRepository, expiredBefore, CHUNK_SIZE_100)
    }

    private fun buildCleanupStep(
        stepName: String,
        reader: ExpiredWithdrawalMemberItemReader,
        writer: MemberCleanupItemWriter
    ): Step {
        return StepBuilder(stepName, jobRepository)
            .chunk<List<Member>, List<Member>>(CHUNK_SIZE_1, transactionManager)
            .reader(reader)
            .writer(writer)
            .build()
    }
}
