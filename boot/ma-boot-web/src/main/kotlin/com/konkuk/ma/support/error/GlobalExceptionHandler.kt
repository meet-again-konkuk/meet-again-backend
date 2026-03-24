package com.konkuk.ma.support.error

import com.konkuk.ma.domain.auth.exception.RefreshTokenExpiredException
import com.konkuk.ma.domain.common.exception.InvalidValueException
import com.konkuk.ma.domain.member.exception.DuplicateEmailException
import com.konkuk.ma.domain.member.exception.DuplicateNicknameException
import com.konkuk.ma.domain.member.exception.PasswordMismatchException
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import com.konkuk.ma.exception.BusinessException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(DuplicateNicknameException::class, DuplicateEmailException::class)
    fun handleDuplicateException(e: BusinessException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)
    }

    @ExceptionHandler(PasswordMismatchException::class, RefreshTokenExpiredException::class)
    fun handleUnauthorizedException(e: BusinessException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
    }

    @ExceptionHandler(InvalidValueException::class, SmsNotVerifiedException::class)
    fun handleBadRequestException(e: BusinessException): ResponseEntity<String> {
        return ResponseEntity.badRequest().body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<String> {
        val message = e.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "Invalid Request"
        return ResponseEntity.badRequest().body(message)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ResponseEntity<String> {
        val maxSize = e.maxUploadSize
        val message = if (maxSize > 0) {
            "파일 크기가 허용 한도(${maxSize / 1024 / 1024}MB)를 초과했습니다."
        } else {
            "파일 크기가 허용 한도를 초과했습니다."
        }
        return ResponseEntity.badRequest().body(message)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        logger.error(e) { "예상하지 못한 에러가 발생했습니다." }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.")
    }
}
