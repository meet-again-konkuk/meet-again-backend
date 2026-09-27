package com.konkuk.ma.domain.point.api

import com.konkuk.ma.domain.point.api.response.PointProductResponse
import com.konkuk.ma.domain.point.application.PointQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/points")
class PointQueryApi(
    private val pointQueryService: PointQueryService,
) {
    @GetMapping
    fun findProducts(): List<PointProductResponse> {
        return pointQueryService.findProducts()
            .map { PointProductResponse.from(it) }
    }
}
