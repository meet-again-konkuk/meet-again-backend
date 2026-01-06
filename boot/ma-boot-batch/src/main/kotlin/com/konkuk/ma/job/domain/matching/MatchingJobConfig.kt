package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.TargetInfo
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
            .chunk<TargetInfo, List<MatchingResult>>(CHUNK_SIZE_100, transactionManager)
            .reader(matchingReader())
            .processor(matchingProcessor())
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
        )
    }

    @Bean
    fun matchingProcessor(): ItemProcessor<TargetInfo, List<MatchingResult>> {
        return ItemProcessor { targetInfo ->
            val members = memberQueryRepository.findByNameAndGender(targetInfo.targetName, targetInfo.targetGender)

            members.filter { member ->
                // 상세 조건 매칭
                val yearMatch = targetInfo.year?.value?.let { it == member.birthDate.year } ?: true
                val monthMatch = targetInfo.month?.value?.let { it == member.birthDate.monthValue } ?: true
                val dayMatch = targetInfo.day?.value?.let { it == member.birthDate.dayOfMonth } ?: true
                val regionMatch = targetInfo.region?.let { it == member.region } ?: true

                // TODO: 전화번호 등 추가 매칭 로직
                yearMatch && monthMatch && dayMatch && regionMatch
            }.map { member ->
                MatchingResult(targetInfo.targetInfoId, member.email)
            }
        }
    }

    @Bean
    fun matchingWriter(): ItemWriter<List<MatchingResult>> {
        return ItemWriter { chunk ->
            // Chunk<List<MatchingResult>> -> Flatten -> List<MatchingResult>
            val flattenedResults = chunk.items.flatten()
            if (flattenedResults.isNotEmpty()) {
                matchingResultRepository.saveAll(flattenedResults)
            }
        }
    }
}
