package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.api.response.MemberPhotoResponse
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoUploader
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val memberPhotoUploader: MemberPhotoUploader
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @AuthenticationPrincipal email: String,
        @RequestPart("photo") photo: MultipartFile
    ): MemberPhotoResponse {
        val photoFile = PhotoFile.create(photo.originalFilename, photo.size, photo.bytes)
        memberPhotoUploader.upload(email, photoFile)
        return MemberPhotoResponse.uploaded()
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    fun deletePhoto(
        @AuthenticationPrincipal email: String
    ): MemberPhotoResponse {
        memberPhotoUploader.delete(email)
        return MemberPhotoResponse.deleted()
    }
}
