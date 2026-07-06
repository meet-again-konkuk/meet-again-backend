package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.report.NewReport

interface ReportCommandRepository {
    fun save(newReport: NewReport): Long
}
