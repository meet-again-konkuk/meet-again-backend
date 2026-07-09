package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.application.LogoutService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class LogoutApi(
    private val logoutService: LogoutService
) {
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@LoginMember memberInfo: MemberInfo) {
        logoutService.logout(memberInfo.id)
    }
}
