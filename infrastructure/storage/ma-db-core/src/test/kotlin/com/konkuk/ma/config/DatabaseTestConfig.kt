package com.konkuk.ma.config

import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.SchemaUtils
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

abstract class DatabaseTestConfig: FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberTable)
        }

        afterEach {
            SchemaUtils.drop(MemberTable)
        }
    }
} 
