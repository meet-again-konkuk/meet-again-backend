package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.job.common.AbstractJobConfig
import com.konkuk.ma.job.common.DateJobParameter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExcludedMatchingResultDeleteJobConfig(
    private val matchingResultRepository: MatchingResultRepository,
    @Qualifier("excludedDateJobParameter") private val dateJobParameter: DateJobParameter
) : AbstractJobConfig() {

    @Bean
    @JobScope
    fun excludedDateJobParameter(): DateJobParameter {
        return DateJobParameter()
    }

    @Bean
    fun excludedMatchingResultDeleteJob(): Job {
        return JobBuilder("excludedMatchingResultDeleteJob", jobRepository)
            .start(excludedMatchingResultDeleteStep())
            .build()
    }

    @Bean
    fun excludedMatchingResultDeleteStep(): Step {
        return StepBuilder("excludedMatchingResultDeleteStep", jobRepository)
            .tasklet(excludedMatchingResultDeleteTasklet(), transactionManager)
            .build()
    }

    @Bean
    fun excludedMatchingResultDeleteTasklet(): Tasklet {
        return Tasklet { _, _ ->
            val cutoffDate = dateJobParameter.inputDate.minusYears(EXCLUDED_RETENTION_YEARS)
            matchingResultRepository.deleteExcludedExpiredMatchingResults(cutoffDate)
            RepeatStatus.FINISHED
        }
    }

    companion object {
        private const val EXCLUDED_RETENTION_YEARS = 1L
    }
}
