package com.konkuk.ma.job

import com.konkuk.ma.logger
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.ListItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class MatchingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
) {
    @Bean
    fun matchingJob(): Job {
        return JobBuilder("matchingJob", jobRepository)
            .start(matchingStep())
            .build()
    }

    @Bean
    fun matchingStep(): Step {
        return StepBuilder("matchingStep", jobRepository)
            .chunk<String, String>(10, transactionManager)
            .reader(matchingReader())
            .processor(matchingProcessor())
            .writer(matchingWriter())
            .build()
    }

    @Bean
    fun matchingReader(): ItemReader<String> {
        return ListItemReader(listOf("item1", "item2", "item3"))
    }

    @Bean
    fun matchingProcessor(): ItemProcessor<String, String> {
        return ItemProcessor { item ->
            logger.info { "Processing $item" }
            item.uppercase()
        }
    }

    @Bean
    fun matchingWriter(): ItemWriter<String> {
        return ItemWriter { items ->
            items.forEach { logger.info { "Writing $it" } }
        }
    }
}
