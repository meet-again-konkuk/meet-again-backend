package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.domain.Targets
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.job.common.AbstractJobConfig
import com.konkuk.ma.job.common.NoOffsetReader
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MatchingJobConfig(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val matchingResultRepository: MatchingResultRepository
) : AbstractJobConfig() {
    @Bean
    fun matchingJob(): Job {
        return JobBuilder("matchingJob", jobRepository)
            .start(matchingStep())
            .build()
    }

    @Bean
    fun matchingStep(): Step {
        return StepBuilder("matchingStep", jobRepository)
            .chunk<TargetInfo, TargetInfo>(CHUNK_SIZE_100, transactionManager)
            .reader(matchingReader())
            .writer(matchingWriter())
            .build()
    }

    @Bean
    @StepScope
    fun matchingReader(): NoOffsetReader<TargetInfo, Long> {
        return object : NoOffsetReader<TargetInfo, Long>(
            chunkSize = CHUNK_SIZE_100,
            readFunction = { cursorId, limit ->
                targetInfoQueryRepository.findNoOffset(cursorId, limit)
            }
        ) {}
    }

    @Bean
    @StepScope
    fun matchingWriter(): ItemWriter<TargetInfo> {
        return ItemWriter { chunk ->
            val targetInfos = TargetInfos(chunk.items)
            val targets = Targets.from(memberQueryRepository.findByNames(targetInfos.targetNames()))
            val matchingResults = targetInfos.makeMatchingResults(targets)

            val existing = matchingResultRepository.findExistingMatchingResults(matchingResults.targetInfoIds())
            val newResults = matchingResults.filterNew(existing)
            matchingResultRepository.saveAll(newResults)
        }
    }
}
