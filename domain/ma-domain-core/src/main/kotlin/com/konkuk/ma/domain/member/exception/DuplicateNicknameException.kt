package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class DuplicateNicknameException(
    nickname: String
) : BusinessException(
    message = "이미 사용중인 닉네임입니다.",
    dataMessage = "nickname: $nickname",
    logLevel = LogLevel.WARN
)
