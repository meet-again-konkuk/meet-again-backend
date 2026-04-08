package com.konkuk.ma.domain.matching.domain

class TargetInfos(
    val data: List<TargetInfo>
) {
    fun extractTargetNames(): Set<String> {
        return data.map { it.targetName }.toSet()
    }

    fun makeMatchingResults(targets: Targets): NewMatchingResults {
        return data.map { targetInfo ->
            targetInfo.makeMatchingResults(targets)
        }.let { NewMatchingResults.merge(it) }
    }
}
