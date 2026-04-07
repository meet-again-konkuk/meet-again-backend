package com.konkuk.ma.job.common

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DateJobParameterConfig {
    @Bean
    @JobScope
    fun dateJobParameter(): DateJobParameter {
        return DateJobParameter()
    }
}
