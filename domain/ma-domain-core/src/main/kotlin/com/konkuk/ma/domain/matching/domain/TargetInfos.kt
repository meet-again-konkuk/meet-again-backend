package com.konkuk.ma.domain.matching.domain

class TargetInfos(
    val data: List<TargetInfo>
) {
    private val nameById: Map<Long, String> by lazy {
        data.associate { it.targetInfoId to it.targetName }
    }

    fun extractTargetNames(): Set<String> {
        return data.map { it.targetName }.toSet()
    }

    fun findName(targetInfoId: Long): String {
        return nameById[targetInfoId] ?: UNKNOWN
    }

    fun makeMatchingResults(targets: Targets): NewMatchingResults {
        return data.map { targetInfo ->
            targetInfo.makeMatchingResults(targets)
        }.let { NewMatchingResults.merge(it) }
    }

    companion object {
        private const val UNKNOWN = "알 수 없음"
    }
}
