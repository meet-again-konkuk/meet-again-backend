package com.konkuk.ma.domain.matching.domain

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun targetInfoIds(): List<Long> {
        return data.map { it.targetInfoId }.distinct()
    }

    fun extractTargetEmails(): Set<String> {
        return data.map { it.targetEmail }.toSet()
    }

    private fun createUniqueKeys(): Set<Pair<Long, String>> {
        return data.map { it.createUniqueKey() }.toSet()
    }

    fun filterNew(existing: MatchingResults): MatchingResults {
        val existingKeys = existing.createUniqueKeys()
        return MatchingResults(data.filter { it.createUniqueKey() !in existingKeys })
    }

    companion object {
        fun merge(dataList: List<MatchingResults>): MatchingResults {
            return MatchingResults(dataList.flatMap { it.data })
        }
    }
}
