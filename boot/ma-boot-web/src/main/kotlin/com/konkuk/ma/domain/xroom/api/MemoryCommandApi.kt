package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.request.AddMemoryRequest
import com.konkuk.ma.domain.xroom.api.request.UpdateMemoryRequest
import com.konkuk.ma.domain.xroom.api.response.MemoryDeleteResponse
import com.konkuk.ma.domain.xroom.api.response.MemoryResponse
import com.konkuk.ma.domain.xroom.application.MemoryCommandService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/xrooms/{xroomId}/memories")
class MemoryCommandApi(
    private val memoryCommandService: MemoryCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addMemory(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @RequestBody request: AddMemoryRequest,
    ): MemoryResponse {
        val memoryId = memoryCommandService.addMemory(xroomId, memberInfo.id, request.toCommand())
        return MemoryResponse(memoryId = memoryId)
    }

    @PatchMapping("/{memoryId}")
    fun updateMemory(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @PathVariable @DecryptId(ObfuscationType.MEMORY) memoryId: Long,
        @RequestBody request: UpdateMemoryRequest,
    ): MemoryResponse {
        val updatedMemoryId = memoryCommandService.updateMemory(xroomId, memoryId, memberInfo.id, request.toCommand())
        return MemoryResponse(memoryId = updatedMemoryId)
    }

    @DeleteMapping("/{memoryId}")
    fun removeMemory(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @PathVariable @DecryptId(ObfuscationType.MEMORY) memoryId: Long,
    ): MemoryDeleteResponse {
        val deletedMemoryId = memoryCommandService.removeMemory(xroomId, memoryId, memberInfo.id)
        return MemoryDeleteResponse(memoryId = deletedMemoryId)
    }
}
