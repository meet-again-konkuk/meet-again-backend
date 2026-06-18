package com.konkuk.ma.job.common

import java.time.LocalDate

open class DateJobParameter(
    inputDate: String?,
) {
    open val inputDate: LocalDate = inputDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
}
