package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResult

interface MatchingResultRepository {
    fun saveAll(matchingResults: List<MatchingResult>)
    fun findExistingKeys(targetInfoIds: List<Long>): Set<Triple<String, Long, String>>
}
