package com.konkuk.ma.job.common

import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager

abstract class AbstractJobConfig {
    @Autowired
    protected lateinit var jobRepository: JobRepository

    @Autowired
    protected lateinit var transactionManager: PlatformTransactionManager

    companion object {
        const val CHUNK_SIZE_100: Int = 100
        const val CHUNK_SIZE: Int = 1000
    }
}
