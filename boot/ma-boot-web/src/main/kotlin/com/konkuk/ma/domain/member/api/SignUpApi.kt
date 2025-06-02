package com.konkuk.ma.domain.member.api

import com.konkuk.ma.member.application.SignUpService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class SignUpApi(
    private val signUpService: SignUpService
) {
    fun signUp() {

    }
}
