package com.konkuk.ma.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class MaEnvironmentPostProcessor : EnvironmentPostProcessor {

    companion object {
        private const val CONFIG_PATH = "classpath*:config/"
        private const val FILE_PATTERN = "application-*"
        private val YAML_EXTENSIONS = listOf(".yml", ".yaml")
        private const val MERGED_PROPERTY_SOURCE_NAME = "ma-merged-config"
        private const val PROFILE_KEY = "spring.config.activate.on-profile"
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val activeProfiles = environment.activeProfiles.toSet()
        if (activeProfiles.isEmpty()) return

        try {
            val mergedProperties = loadAndMergeConfigurations(activeProfiles)
            if (mergedProperties.isNotEmpty()) {
                addMergedPropertySource(environment, mergedProperties)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load ASM configuration files", e)
        }
    }

    /**
     * 활성 프로파일에 해당하는 설정 파일들을 찾아서 병합
     */
    private fun loadAndMergeConfigurations(activeProfiles: Set<String>): Map<String, Any> {
        val resolver = PathMatchingResourcePatternResolver()
        val mergedProperties = mutableMapOf<String, Any>()

        YAML_EXTENSIONS.forEach { extension ->
            val appPattern = "classpath*:application" + extension
            val appResources = resolver.getResources(appPattern)

            appResources.forEach { resource ->
                mergeCommonProperties(resource, mergedProperties)
            }
        }

        YAML_EXTENSIONS.forEach { extension ->
            val commonPattern = CONFIG_PATH + "application" + extension
            val commonResources = resolver.getResources(commonPattern)

            commonResources.forEach { resource ->
                mergeCommonProperties(resource, mergedProperties)
            }
        }

        YAML_EXTENSIONS.forEach { extension ->
            val searchPattern = CONFIG_PATH + FILE_PATTERN + extension
            val resources = resolver.getResources(searchPattern)

            resources.forEach { resource ->
                mergeResourceProperties(resource, activeProfiles, mergedProperties)
            }
        }

        return mergedProperties
    }

    private fun mergeCommonProperties(
        resource: Resource,
        mergedProperties: MutableMap<String, Any>
    ) {
        val loader = YamlPropertySourceLoader()
        val propertySources = loader.load(resource.filename, resource)

        propertySources.forEach { propertySource ->
            val sourceMap = propertySource.source as Map<*, *>

            // 공통 설정은 프로파일 체크 없이 모두 로드
            sourceMap.forEach { (key, value) ->
                val keyStr = key.toString()
                // spring.config.activate.on-profile 키는 제외
                if (keyStr != PROFILE_KEY) {
                    // 로딩 순서가 보장되므로 나중에 로드된 값이 우선 (app 모듈이 마지막)
                    mergedProperties[keyStr] = value.toString()
                }
            }
        }
    }

    /**
     * 개별 리소스 파일의 설정을 병합 맵에 추가
     */
    private fun mergeResourceProperties(
        resource: Resource,
        activeProfiles: Set<String>,
        mergedProperties: MutableMap<String, Any>
    ) {
        val loader = YamlPropertySourceLoader()
        val propertySources = loader.load(resource.filename, resource)

        propertySources.forEach { propertySource ->
            val sourceMap = propertySource.source as Map<*, *>
            val profileInFile = sourceMap[PROFILE_KEY]?.toString()

            // 활성 프로파일과 매칭되는 설정만 병합
            if (profileInFile in activeProfiles) {
                sourceMap.forEach { (key, value) ->
                    val keyStr = key.toString()
                    // 프로파일별 설정은 공통 설정을 오버라이드 (덮어쓰기 허용)
                    mergedProperties[keyStr] = value.toString()
                }
            }
        }
    }



    /**
     * 병합된 설정을 Spring Environment에 PropertySource로 추가
     */
    private fun addMergedPropertySource(
        environment: ConfigurableEnvironment,
        mergedProperties: Map<String, Any>
    ) {
        val mergedPropertySource = MapPropertySource(MERGED_PROPERTY_SOURCE_NAME, mergedProperties)
        environment.propertySources.addFirst(mergedPropertySource)
    }
}
