package com.konkuk.ma.job.domain.community

import com.konkuk.ma.domain.community.domain.image.PostImage
import com.konkuk.ma.domain.community.domain.image.PostImagePurger
import com.konkuk.ma.domain.community.domain.image.policy.PostImagePurgePolicy
import com.konkuk.ma.domain.community.domain.port.PostImageQueryRepository
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
class DeletedPostImagePurgeJobConfig(
    private val postImageQueryRepository: PostImageQueryRepository,
    private val postImagePurger: PostImagePurger,
    private val dateJobParameter: DateJobParameter,
) : AbstractJobConfig() {

    @Bean
    fun deletedPostImagePurgeJob(deletedPostImagePurgeStep: Step): Job {
        return JobBuilder("deletedPostImagePurgeJob", jobRepository)
            .start(deletedPostImagePurgeStep)
            .build()
    }

    @Bean
    fun deletedPostImagePurgeStep(reader: DeletedPostImageItemReader): Step {
        return StepBuilder("deletedPostImagePurgeStep", jobRepository)
            .chunk<PostImage, PostImage>(CHUNK_SIZE_100, transactionManager)
            .reader(reader)
            .writer(PostImagePurgeItemWriter(postImagePurger))
            .build()
    }

    @Bean
    @StepScope
    fun deletedPostImagePurgeReader(): DeletedPostImageItemReader {
        val purgeCutoff = PostImagePurgePolicy.purgeCutoff(dateJobParameter.inputDate)
        return DeletedPostImageItemReader(postImageQueryRepository, purgeCutoff, CHUNK_SIZE_100)
    }
}
