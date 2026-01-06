package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResult

interface MatchingResultRepository {
    fun saveAll(matchingResults: List<MatchingResult>)
}
