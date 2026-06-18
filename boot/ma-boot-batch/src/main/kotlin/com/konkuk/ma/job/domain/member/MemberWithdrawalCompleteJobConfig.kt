package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.withdrawal.domain.MemberBackupArchiver
import com.konkuk.ma.domain.withdrawal.domain.MemberDataCleaner
import com.konkuk.ma.domain.withdrawal.domain.policy.WithdrawalPolicy
import com.konkuk.ma.job.common.AbstractJobConfig
import com.konkuk.ma.job.common.DateJobParameter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MemberWithdrawalCompleteJobConfig(
    private val memberQueryRepository: MemberQueryRepository,
    private val backupArchiver: MemberBackupArchiver,
    private val dataCleaner: MemberDataCleaner,
    private val dateJobParameter: DateJobParameter
) : AbstractJobConfig() {

    @Bean
    fun memberWithdrawalCompleteJob(memberWithdrawalCleanupStep: Step): Job {
        return JobBuilder("memberWithdrawalCompleteJob", jobRepository)
            .start(memberWithdrawalCleanupStep)
            .build()
    }

    @Bean
    fun memberWithdrawalCleanupStep(reader: ExpiredWithdrawalMemberItemReader): Step {
        return StepBuilder("memberWithdrawalCleanupStep", jobRepository)
            .chunk<Member, Member>(CHUNK_SIZE_20, transactionManager)
            .reader(reader)
            .processor(MemberBackupItemProcessor(backupArchiver))
            .writer(MemberPurgeItemWriter(dataCleaner))
            .build()
    }

    @Bean
    @StepScope
    fun memberWithdrawalCleanupReader(): ExpiredWithdrawalMemberItemReader {
        val expiredBefore = WithdrawalPolicy.expirationCutoff(dateJobParameter.inputDate)
        return ExpiredWithdrawalMemberItemReader(memberQueryRepository, expiredBefore, CHUNK_SIZE_20)
    }
}
