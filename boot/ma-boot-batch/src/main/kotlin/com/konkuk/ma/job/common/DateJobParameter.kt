package com.konkuk.ma.job.common

import org.springframework.beans.factory.annotation.Value
import java.time.LocalDate

class DateJobParameter {
    lateinit var inputDate: LocalDate
        private set

    @Value("#{jobParameters['inputDate']}")
    fun setInputDate(inputDate: String?) {
        this.inputDate = inputDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
    }
}
