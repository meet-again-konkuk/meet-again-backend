package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.domain.Targets
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.job.common.AbstractJobConfig
import com.konkuk.ma.job.common.NoOffsetListReader
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
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
            .chunk<List<TargetInfo>, List<MatchingResult>>(CHUNK_SIZE_1, transactionManager)
            .reader(matchingReader())
            .processor(matchingProcessor())
            .writer(matchingWriter())
            .build()
    }

    @Bean
    @StepScope
    fun matchingReader(): NoOffsetListReader<TargetInfo, Long> {
        return object : NoOffsetListReader<TargetInfo, Long>(
            readSize = CHUNK_SIZE_100,
            readFunction = { cursorId, limit ->
                targetInfoQueryRepository.findNoOffset(cursorId, limit)
            },
            cursorIdExtractor = { it.targetInfoId }
        ) {}
    }

    @Bean
    @StepScope
    fun matchingProcessor(): ItemProcessor<List<TargetInfo>, List<MatchingResult>> {
        return ItemProcessor { targetInfoList ->
            val targetInfos = TargetInfos(targetInfoList)
            val targets = Targets.from(memberQueryRepository.findByNames(targetInfos.extractTargetNames()))
            val matchingResults = targetInfos.makeMatchingResults(targets)
            val existing = MatchingResults(matchingResultRepository.findExistingMatchingResults(matchingResults.targetInfoIds()))
            matchingResults.filterNew(existing).data
        }
    }

    @Bean
    @StepScope
    fun matchingWriter(): ItemWriter<List<MatchingResult>> {
        return ItemWriter { chunk ->
            chunk.items.forEach { matchingResults ->
                matchingResultRepository.saveAll(matchingResults)
            }
        }
    }
}
