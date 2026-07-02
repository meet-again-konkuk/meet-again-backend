package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.response.MediaUploadResponse
import com.konkuk.ma.domain.xroom.api.response.MemoryPhotoDeleteResponse
import com.konkuk.ma.domain.xroom.application.MemoryPhotoCommandService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/xrooms/{xroomId}/memories/{memoryId}/photo")
class MemoryPhotoApi(
    private val memoryPhotoCommandService: MemoryPhotoCommandService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @PathVariable @DecryptId(ObfuscationType.MEMORY) memoryId: Long,
        @RequestPart("photo") photo: MultipartFile,
    ): MediaUploadResponse {
        val photoFile = PhotoFile.create(photo.originalFilename, photo.size, photo.bytes)
        val uploadResult = memoryPhotoCommandService.uploadPhoto(xroomId, memoryId, memberInfo.id, photoFile)
        return MediaUploadResponse.from(uploadResult)
    }

    @DeleteMapping
    fun removePhoto(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @PathVariable @DecryptId(ObfuscationType.MEMORY) memoryId: Long,
    ): MemoryPhotoDeleteResponse {
        memoryPhotoCommandService.removePhoto(xroomId, memoryId, memberInfo.id)
        return MemoryPhotoDeleteResponse(memoryId = memoryId)
    }
}
