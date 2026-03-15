package com.konkuk.ma.domain

import com.konkuk.ma.domain.auth.domain.SmsVerification
import com.konkuk.ma.domain.auth.domain.port.SmsSender
import net.nurigo.sdk.message.model.Message
import net.nurigo.sdk.message.service.DefaultMessageService
import org.springframework.stereotype.Component

@Component
class CoolSmsSender(
    private val messageService: DefaultMessageService,
    @org.springframework.beans.factory.annotation.Value("\${sms.sender.phone-number}")
    private val senderPhoneNumber: String,
) : SmsSender {

    companion object {
        private const val VERIFICATION_CODE_MIN = 100000
        private const val VERIFICATION_CODE_MAX = 999999
    }

    override fun sendSmsVerificationCode(phoneNumber: String): SmsVerification {
        val verificationCode = (VERIFICATION_CODE_MIN..VERIFICATION_CODE_MAX).random()
        val message = Message(
            from = senderPhoneNumber,
            to = phoneNumber,
            text = "본인확인 인증번호는 $verificationCode 입니다."
        )
        //messageService.sendOne(SingleMessageSendingRequest(message))
        return SmsVerification(phoneNumber, verificationCode)
    }
}
