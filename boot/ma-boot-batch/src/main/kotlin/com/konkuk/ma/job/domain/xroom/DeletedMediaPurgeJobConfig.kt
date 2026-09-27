package com.konkuk.ma.job.domain.xroom

import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.media.MediaPurger
import com.konkuk.ma.domain.xroom.domain.media.policy.MediaPurgePolicy
import com.konkuk.ma.domain.xroom.domain.port.MediaQueryRepository
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
class DeletedMediaPurgeJobConfig(
    private val mediaQueryRepository: MediaQueryRepository,
    private val mediaPurger: MediaPurger,
    private val dateJobParameter: DateJobParameter,
) : AbstractJobConfig() {

    @Bean
    fun deletedMediaPurgeJob(deletedMediaPurgeStep: Step): Job {
        return JobBuilder("deletedMediaPurgeJob", jobRepository)
            .start(deletedMediaPurgeStep)
            .build()
    }

    @Bean
    fun deletedMediaPurgeStep(reader: DeletedMediaItemReader): Step {
        return StepBuilder("deletedMediaPurgeStep", jobRepository)
            .chunk<Media, Media>(CHUNK_SIZE_100, transactionManager)
            .reader(reader)
            .writer(MediaPurgeItemWriter(mediaPurger))
            .build()
    }

    @Bean
    @StepScope
    fun deletedMediaPurgeReader(): DeletedMediaItemReader {
        val purgeCutoff = MediaPurgePolicy.purgeCutoff(dateJobParameter.inputDate)
        return DeletedMediaItemReader(mediaQueryRepository, purgeCutoff, CHUNK_SIZE_100)
    }
}
