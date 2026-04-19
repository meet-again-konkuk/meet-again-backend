package com.konkuk.ma.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.point.entity.CachedAmountDiscount
import com.konkuk.ma.domain.point.entity.CachedDiscount
import com.konkuk.ma.domain.point.entity.CachedPercentDiscount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

class CachedDiscountJacksonConfigTest : FunSpec({

    val mapper: ObjectMapper = Jackson2ObjectMapperBuilder.json()
        .let { builder ->
            CachedDiscountJacksonConfig().cachedDiscountJacksonCustomizer().customize(builder)
            builder.build<ObjectMapper>()
        }

    test("CachedAmountDiscount 직렬화/역직렬화") {
        val original: CachedDiscount = CachedAmountDiscount(
            startDate = "2026-04-01",
            endDate = "2026-05-01",
            discountAmount = 200,
        )

        val json = mapper.writeValueAsString(original)
        val decoded = mapper.readValue(json, CachedDiscount::class.java)

        decoded.shouldBeInstanceOf<CachedAmountDiscount>()
        decoded shouldBe original
    }

    test("CachedPercentDiscount 직렬화/역직렬화") {
        val original: CachedDiscount = CachedPercentDiscount(
            startDate = "2026-04-01",
            endDate = "2026-05-01",
            discountPercent = 20,
        )

        val json = mapper.writeValueAsString(original)
        val decoded = mapper.readValue(json, CachedDiscount::class.java)

        decoded.shouldBeInstanceOf<CachedPercentDiscount>()
        decoded shouldBe original
    }

    test("JSON에 type 디스크리미네이터가 포함된다") {
        val original: CachedDiscount = CachedAmountDiscount(
            startDate = "2026-04-01",
            endDate = "2026-05-01",
            discountAmount = 100,
        )

        val json = mapper.writeValueAsString(original)

        json.contains("\"type\":\"AMOUNT\"") shouldBe true
    }
})
