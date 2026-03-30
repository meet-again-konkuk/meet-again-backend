package com.konkuk.ma.domain.matching.domain

import kotlin.reflect.KClass

class MatchRateCalculator(
    private val groups: List<MatchingGroup>,
    private val regionMatched: Boolean,
) {

    fun calculate(): Int {
        val fullMatchedTypes = findFullMatchedTypes()
        if (fullMatchedTypes.isNotEmpty()) {
            return findBonusRate(fullMatchedTypes, regionMatched)
        }
        return sumPartialRates() + calculateRegionRate()
    }

    private fun findFullMatchedTypes(): Set<KClass<out MatchingGroup>> =
        groups.filter { it.isFullMatched() }
            .map { it::class }
            .toSet()

    private fun findBonusRate(
        fullMatchedTypes: Set<KClass<out MatchingGroup>>,
        regionMatched: Boolean,
    ): Int {
        val key = BonusKey(fullMatchedTypes, regionMatched)
        return FULL_MATCH_BONUS_TABLE[key] ?: NO_RATE
    }

    private fun sumPartialRates(): Int =
        groups.sumOf { it.calculatePartialRate() }

    private fun calculateRegionRate(): Int =
        if (regionMatched) REGION_RATE else NO_RATE

    private data class BonusKey(
        val fullMatchedTypes: Set<KClass<out MatchingGroup>>,
        val regionMatched: Boolean,
    )

    companion object {
        private const val NO_RATE = 0
        private const val REGION_RATE = 10

        private const val PHONE_AND_BIRTH_WITH_REGION = 99
        private const val PHONE_AND_BIRTH = 97
        private const val PHONE_WITH_REGION = 90
        private const val PHONE_ONLY = 85
        private const val BIRTH_WITH_REGION = 83
        private const val BIRTH_ONLY = 80

        private val FULL_MATCH_BONUS_TABLE: Map<BonusKey, Int> = mapOf(
            BonusKey(setOf(MatchingGroup.Phone::class, MatchingGroup.Birth::class), true) to PHONE_AND_BIRTH_WITH_REGION,
            BonusKey(setOf(MatchingGroup.Phone::class, MatchingGroup.Birth::class), false) to PHONE_AND_BIRTH,
            BonusKey(setOf(MatchingGroup.Phone::class), true) to PHONE_WITH_REGION,
            BonusKey(setOf(MatchingGroup.Phone::class), false) to PHONE_ONLY,
            BonusKey(setOf(MatchingGroup.Birth::class), true) to BIRTH_WITH_REGION,
            BonusKey(setOf(MatchingGroup.Birth::class), false) to BIRTH_ONLY,
        )
    }
}
