package com.konkuk.ma.domain.mock

import com.konkuk.ma.domain.auth.domain.VerificationCode
import com.konkuk.ma.domain.auth.domain.port.SmsSender
import com.konkuk.ma.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local", "test")
class LocalSmsSender : SmsSender {

    override fun send(phoneNumber: String, code: VerificationCode) {
        logger.info { "[LOCAL SMS] phone=$phoneNumber code=${code.value}" }
    }
}
