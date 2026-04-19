package com.konkuk.ma.domain.common.domain

import java.math.BigDecimal
import java.math.RoundingMode

class Money private constructor(
    val amount: BigDecimal,
) {
    operator fun plus(other: Money): Money = Money(amount.add(other.amount))

    operator fun minus(other: Money): Money = Money(amount.subtract(other.amount))

    operator fun times(percent: Double): Money = Money(amount.multiply(BigDecimal.valueOf(percent)))

    fun times(numerator: Int, denominator: Int): Money {
        return Money(amount.multiply(BigDecimal.valueOf(numerator.toLong())).divide(BigDecimal.valueOf(denominator.toLong()), 0, RoundingMode.DOWN))
    }

    fun isLessThan(other: Money): Boolean = amount < other.amount

    fun isGreaterThanOrEqualTo(other: Money): Boolean = amount >= other.amount

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    /**
     * 이 금액이 base 금액에서 차지하는 비율을 정수 퍼센트(%)로 반환한다.
     * base가 0이면 0을 반환한다. 소수점 이하는 버림.
     */
    fun percentageOf(base: Money): Int {
        if (base.isZero()) return 0
        return amount.multiply(BigDecimal(100))
            .divide(base.amount, 0, RoundingMode.DOWN)
            .toInt()
    }

    fun toLong(): Long = amount.toLong()

    fun toInt(): Int = amount.toInt()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        return amount.compareTo(other.amount) == 0
    }

    override fun hashCode(): Int = amount.stripTrailingZeros().hashCode()

    override fun toString(): String = "${amount.toPlainString()}원"

    companion object {
        val ZERO: Money = Money(BigDecimal.ZERO)

        fun wons(amount: Long): Money = Money(BigDecimal.valueOf(amount))

        fun wons(amount: Int): Money = Money(BigDecimal.valueOf(amount.toLong()))
    }
}
