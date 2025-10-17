package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.support.payload.response.EnumDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberEnumApi {
    @GetMapping("/regions")
    fun getRegionEnums(): List<EnumDto<Region>> {
        return EnumDto.toEnumDtos(Region::class)
    }
}
