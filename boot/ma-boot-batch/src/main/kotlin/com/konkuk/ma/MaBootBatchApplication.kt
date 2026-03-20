package com.konkuk.ma

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication
@ComponentScan(
    basePackages = ["com.konkuk.ma"],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ["com.konkuk.ma.domain.auth.*"]
    )]
)
class MaBootBatchApplication

fun main(args: Array<String>) {
    runApplication<MaBootBatchApplication>(*args)
}
