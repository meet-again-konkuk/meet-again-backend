package com.konkuk.ma.job.common

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DateJobParameterConfig {
    @Bean
    @JobScope
    fun dateJobParameter(
        @Value("#{jobParameters['inputDate']}") inputDate: String?,
    ): DateJobParameter {
        return DateJobParameter(inputDate)
    }
}
