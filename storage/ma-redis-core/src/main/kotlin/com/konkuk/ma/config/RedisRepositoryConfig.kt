package com.konkuk.ma.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableRedisRepositories
class RedisRepositoryConfig(
    @Value("\${spring.data.redis.host}")
    private val redisHost: String,

    @Value("\${spring.data.redis.port}")
    private val redisPort: Int
) {
    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory(RedisStandaloneConfiguration(redisHost, redisPort))
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> {
        val redisTemplate = RedisTemplate<String, Any>()
        redisTemplate.connectionFactory = redisConnectionFactory()
        val serializer = StringRedisSerializer()
        redisTemplate.keySerializer = serializer
        redisTemplate.valueSerializer = serializer
        redisTemplate.hashKeySerializer = serializer
        redisTemplate.hashValueSerializer = serializer
        redisTemplate.setEnableTransactionSupport(true)

        return redisTemplate
    }
}
