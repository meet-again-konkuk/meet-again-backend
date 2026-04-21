package com.konkuk.ma.domain.point.domain.balance

import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.exception.InvalidValueException

class PointQuantity(val value: Int) {
    init {
        if (value < 0) {
            throw InvalidValueException(PointQuantity::class, value, "인연 수량은 음수일 수 없습니다.")
        }
    }

    operator fun plus(other: PointQuantity): PointQuantity {
        return PointQuantity(value + other.value)
    }

    operator fun minus(other: PointQuantity): PointQuantity {
        if (isLessThan(other)) {
            throw InvalidStateException(PointQuantity::class, value, "인연 잔액이 부족합니다.")
        }
        return PointQuantity(value - other.value)
    }

    fun isLessThan(other: PointQuantity): Boolean = value < other.value

    fun isZero(): Boolean = value == 0

    fun toInt(): Int = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointQuantity) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "${value}개"

    companion object {
        val ZERO: PointQuantity = PointQuantity(0)
    }
}
