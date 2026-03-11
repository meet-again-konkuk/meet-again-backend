package com.konkuk.ma.domain.matching.domain

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun targetInfoIds(): List<Long> {
        return data.map { it.targetInfoId }.distinct()
    }

    fun filterNew(existing: MatchingResults): MatchingResults {
        val existingKeys = existing.data.map { it.uniqueKey() }.toSet()
        return MatchingResults(data.filter { it.uniqueKey() !in existingKeys })
    }

    companion object {
        fun merge(dataList: List<MatchingResults>): MatchingResults {
            return MatchingResults(dataList.flatMap { it.data })
        }
    }
}
