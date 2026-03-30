package com.konkuk.ma.domain.matching.domain

sealed class MatchingGroup {

    abstract fun isFullMatched(): Boolean

    abstract fun calculatePartialRate(): Int

    class Phone(
        private val middleNumberMatched: Boolean,
        private val lastNumberMatched: Boolean,
    ) : MatchingGroup() {

        override fun isFullMatched(): Boolean = middleNumberMatched && lastNumberMatched

        override fun calculatePartialRate(): Int =
            if (hasAnyMatched()) PARTIAL_RATE else NO_RATE

        private fun hasAnyMatched(): Boolean = middleNumberMatched || lastNumberMatched

        companion object {
            private const val PARTIAL_RATE = 30
            private const val NO_RATE = 0
        }
    }

    class Birth(
        private val yearMatched: Boolean,
        private val monthMatched: Boolean,
        private val dayMatched: Boolean,
    ) : MatchingGroup() {

        override fun isFullMatched(): Boolean = yearMatched && monthMatched && dayMatched

        override fun calculatePartialRate(): Int = countMatchedItems() * ITEM_RATE

        private fun countMatchedItems(): Int =
            listOf(yearMatched, monthMatched, dayMatched).count { it }

        companion object {
            private const val ITEM_RATE = 10
        }
    }
}
