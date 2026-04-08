package com.konkuk.ma.domain.matching.domain

class NewMatchingResults(
    val data: List<NewMatchingResult>
) {
    fun targetInfoIds(): List<Long> {
        return data.map { it.targetInfoId }.distinct()
    }

    fun filterNew(existing: List<MatchingResult>): NewMatchingResults {
        val existingKeys = existing.map { it.createUniqueKey() }.toSet()
        return NewMatchingResults(data.filter { it.createUniqueKey() !in existingKeys })
    }

    companion object {
        fun merge(dataList: List<NewMatchingResults>): NewMatchingResults {
            return NewMatchingResults(dataList.flatMap { it.data })
        }
    }
}
