package com.konkuk.ma.domain.point.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.PointQueryService
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.discountRate
import com.konkuk.ma.vocabulary.discountType
import com.konkuk.ma.vocabulary.discountedPrice
import com.konkuk.ma.vocabulary.isDiscountActive
import com.konkuk.ma.vocabulary.pointProductId
import com.konkuk.ma.vocabulary.pointProductName
import com.konkuk.ma.vocabulary.price
import com.konkuk.ma.vocabulary.quantity
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDate

@WebMvcTest(PointQueryApi::class)
@BaseApiTest
class PointQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val pointQueryService: PointQueryService,
) : FunSpec({

    test("포인트 상품 목록 조회 API 문서화") {
        // Given
        every { pointQueryService.findProducts() } returns listOf(
            PointProductWithDiscount(
                pointProduct = PointProduct(
                    pointProductId = 1L,
                    name = "인연 10개",
                    quantity = 10,
                    price = Money.wons(1000),
                    displayOrder = 1,
                    discountPolicyId = 1L,
                ),
                discountPolicy = AmountDiscountPolicy(
                    discountPolicyId = 1L,
                    startDate = LocalDate.now().minusDays(1),
                    endDate = LocalDate.now().plusDays(30),
                    discountAmount = Money.wons(200),
                ),
            ),
            PointProductWithDiscount(
                pointProduct = PointProduct(
                    pointProductId = 2L,
                    name = "인연 30개",
                    quantity = 30,
                    price = Money.wons(2500),
                    displayOrder = 2,
                ),
                discountPolicy = null,
            ),
            PointProductWithDiscount(
                pointProduct = PointProduct(
                    pointProductId = 3L,
                    name = "인연 50개",
                    quantity = 50,
                    price = Money.wons(4000),
                    displayOrder = 3,
                ),
                discountPolicy = null,
            ),
        )

        // When & Then
        mockMvc.getJson("/api/points") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "point/find-products",
                responseBody(
                    pointProductId("[].pointProductId"),
                    pointProductName("[].name"),
                    quantity("[].quantity"),
                    price("[].price"),
                    discountedPrice("[].discountedPrice"),
                    discountRate("[].discountRate"),
                    discountType("[].discountType") isOptional true,
                    isDiscountActive("[].isDiscountActive"),
                ),
            )
    }

    test("포인트 상품이 없으면 빈 목록을 반환한다") {
        // Given
        every { pointQueryService.findProducts() } returns emptyList()

        // When & Then
        mockMvc.getJson("/api/points") {}
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$") { isArray() } }
            .andExpect { jsonPath("$.length()") { value(0) } }
    }
})
