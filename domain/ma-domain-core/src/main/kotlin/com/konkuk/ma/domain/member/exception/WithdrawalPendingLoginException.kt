package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.BusinessException

class WithdrawalPendingLoginException(
    email: Email
) : BusinessException(
    message = "탈퇴 신청 상태의 회원입니다. 탈퇴 해제 후 로그인할 수 있습니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
