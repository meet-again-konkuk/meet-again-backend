package com.konkuk.ma.domain

import com.konkuk.ma.domain.auth.domain.VerificationCode
import com.konkuk.ma.domain.auth.domain.port.SmsSender
import com.konkuk.ma.logger
import net.nurigo.sdk.message.model.Message
import net.nurigo.sdk.message.request.SingleMessageSendingRequest
import net.nurigo.sdk.message.service.DefaultMessageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("prod")
class CoolSmsSender(
    private val messageService: DefaultMessageService,
    @Value("\${sms.sender.phone-number}")
    private val senderPhoneNumber: String,
) : SmsSender {

    override fun send(phoneNumber: String, code: VerificationCode) {
        val message = Message(
            from = senderPhoneNumber,
            to = phoneNumber,
            text = "본인확인 인증번호는 ${code.value} 입니다."
        )
        val response = messageService.sendOne(SingleMessageSendingRequest(message))
        logger.info { "CoolSMS 발송 완료: phone=$phoneNumber, statusCode=${response?.statusCode}" }
    }
}
