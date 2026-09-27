package com.konkuk.ma.config

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class TestDatabaseConfig {
    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return SpringTransactionManager(dataSource)
    }
}
