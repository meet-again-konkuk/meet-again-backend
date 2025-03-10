package com.konkuk.ma.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class MaEnvironmentPostProcessor : EnvironmentPostProcessor {

    companion object {
        private const val FILE_PATH = "classpath*:config/"
        private const val FILE_PATTERN = "application-*"
        private val YAML_EXTENSIONS = listOf(".yml", ".yaml")
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val sources = environment.propertySources
        val activeProfiles = environment.activeProfiles
        val resolver = PathMatchingResourcePatternResolver()

        try {
            val resources = mutableListOf<Resource>()
            YAML_EXTENSIONS.forEach {
                resources.addAll(resolver.getResources(FILE_PATH + FILE_PATTERN + it))
            }
            resources.forEach { resource ->
                YamlPropertySourceLoader().load(resource.filename, resource)
                    .filter {
                        activeProfiles.any { activeProfile -> activeProfile == (it.source as Map<*, *>)["spring.config.activate.on-profile"]?.toString() }
                    }
                    .forEach {
                        sources.addLast(it)
                    }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load configuration files", e)
        }
    }
}
