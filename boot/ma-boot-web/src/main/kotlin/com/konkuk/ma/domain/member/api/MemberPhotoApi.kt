package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.api.response.MemberPhotoResponse
import com.konkuk.ma.domain.member.application.MemberPhotoService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/members/photos")
class MemberPhotoApi(
    private val memberPhotoService: MemberPhotoService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @LoginMember memberInfo: MemberInfo,
        @RequestPart("photo") photo: MultipartFile
    ): MemberPhotoResponse {
        val photoFile = PhotoFile.create(photo.originalFilename, photo.size, photo.bytes)
        memberPhotoService.upload(memberInfo.email, photoFile)
        return MemberPhotoResponse.uploaded()
    }

    @DeleteMapping
    fun deletePhoto(
        @LoginMember memberInfo: MemberInfo
    ): MemberPhotoResponse {
        memberPhotoService.delete(memberInfo.email)
        return MemberPhotoResponse.deleted()
    }
}
