package com.konkuk.ma.domain.matching.domain

class TargetInfos(
    val data: List<TargetInfo>
) {
    fun extractTargetNames(): Set<String> {
        return data.map { it.targetName }.toSet()
    }

    fun makeMatchingResults(targets: Targets): MatchingResults {
        return data.map { targetInfo ->
            targetInfo.makeMatchingResults(targets)
        }.let { MatchingResults.merge(it) }
    }
}
