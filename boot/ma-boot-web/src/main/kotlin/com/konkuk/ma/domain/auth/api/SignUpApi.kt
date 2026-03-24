package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.SignUpRequest
import com.konkuk.ma.domain.auth.api.response.SignUpResponse
import com.konkuk.ma.domain.auth.application.SignUpService
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/auth")
class SignUpApi(
    private val signUpService: SignUpService
) {
    @PostMapping("/sign-up", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun signUp(
        @Valid @RequestPart("request") request: SignUpRequest,
        @RequestPart("photo", required = false) photo: MultipartFile?
    ): SignUpResponse {
        val photoFile = photo?.let { PhotoFile.create(it.originalFilename, it.size, it.bytes) }
        val memberId = signUpService.signUp(request.toCommand(), photoFile)

        return SignUpResponse(
            memberId = memberId,
            email = request.email,
            nickname = request.nickname
        )
    }
}
