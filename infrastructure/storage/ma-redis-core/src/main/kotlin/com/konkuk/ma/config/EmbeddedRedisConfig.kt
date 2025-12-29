package com.konkuk.ma.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import redis.embedded.RedisServer

@Configuration
@Profile("local", "test")
class EmbeddedRedisConfig(
    @Value("\${spring.data.redis.port}")
    private var redisPort: Int
) {
    private var redisServer: RedisServer? = null

    private val log = LoggerFactory.getLogger(EmbeddedRedisConfig::class.java)

    @PostConstruct
    private fun start() {
        val port = if (isRedisRunning()) findAvailablePort() else redisPort
        redisServer = RedisServer(port)
        redisServer?.start()
    }

    @PreDestroy
    private fun stop() {
        redisServer?.stop()
    }

    private fun isRedisRunning(): Boolean {
        return isRunning(executeGrepProcessCommand(redisPort))
    }

    fun findAvailablePort(): Int {
        for (port in 10000..65535) {
            val process = executeGrepProcessCommand(port)
            if (!isRunning(process)) {
                return port
            }
        }
        throw IllegalArgumentException("Not Found Available port: 10000 ~ 65535")
    }

    @Throws(IOException::class)
    private fun executeGrepProcessCommand(port: Int): Process {
        val command = "netstat -nat | grep LISTEN | grep $port"
        val shell = arrayOf("/bin/sh", "-c", command)
        return Runtime.getRuntime().exec(shell)
    }

    private fun isRunning(process: Process): Boolean {
        val pidInfo = StringBuilder()
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    pidInfo.append(line)
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            log.error(e.message)
        }
        return pidInfo.isNotEmpty()
    }
}
