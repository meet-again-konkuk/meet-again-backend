package com.konkuk.ma.domain.matching.domain

class MatchingResultWithProfile(
    val matchingResult: MatchingResult,
    val targetName: String,
    val targetNickname: String,
    val profileImageUrl: String?,
)
