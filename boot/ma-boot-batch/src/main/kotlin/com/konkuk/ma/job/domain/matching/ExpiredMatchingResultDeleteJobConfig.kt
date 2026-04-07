package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.job.common.AbstractJobConfig
import com.konkuk.ma.job.common.DateJobParameter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExpiredMatchingResultDeleteJobConfig(
    private val matchingResultRepository: MatchingResultRepository,
    private val dateJobParameter: DateJobParameter
) : AbstractJobConfig() {

    @Bean
    fun expiredMatchingResultDeleteJob(): Job {
        return JobBuilder("expiredMatchingResultDeleteJob", jobRepository)
            .start(expiredMatchingResultDeleteStep())
            .build()
    }

    @Bean
    fun expiredMatchingResultDeleteStep(): Step {
        return StepBuilder("expiredMatchingResultDeleteStep", jobRepository)
            .tasklet(expiredMatchingResultDeleteTasklet(), transactionManager)
            .build()
    }

    @Bean
    fun expiredMatchingResultDeleteTasklet(): Tasklet {
        return Tasklet { _, _ ->
            matchingResultRepository.deleteExpiredMatchingResults(dateJobParameter.inputDate)
            RepeatStatus.FINISHED
        }
    }
}
