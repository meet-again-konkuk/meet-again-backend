package com.konkuk.ma.support.payload.response

import com.konkuk.ma.domain.common.domain.EnumWithDisplayName
import kotlin.reflect.KClass

data class EnumDto<T : Enum<T>>(
    val category: T,
    val displayName: String
) {
    companion object {
        inline fun <reified T> toEnumDtos(enumType: KClass<T>): List<EnumDto<T>>
            where T : Enum<T>, T : EnumWithDisplayName =
            enumType.java.enumConstants.map { EnumDto(it, it.displayName) }
    }
}
