package com.konkuk.ma.domain

import com.konkuk.ma.domain.auth.domain.SmsVerification
import com.konkuk.ma.domain.auth.domain.port.SmsSender
import net.nurigo.sdk.message.model.Message
import net.nurigo.sdk.message.service.DefaultMessageService
import org.springframework.stereotype.Component

@Component
class CoolSmsSender(
    private val messageService: DefaultMessageService
) : SmsSender {
    override fun sendSmsVerificationCode(phoneNumber: String): SmsVerification {
        val verificationCode = (100000..999999).random()
        print("verification code $verificationCode")
        val message = Message(
            from = "01026643927",
            to = phoneNumber,
            text = "본인확인 인증번호는 $verificationCode 입니다."
        )
        //messageService.sendOne(SingleMessageSendingRequest(message))
        return SmsVerification(phoneNumber, verificationCode)
    }
}
