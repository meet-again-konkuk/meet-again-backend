package com.konkuk.ma.support.error

import com.konkuk.ma.domain.auth.exception.RefreshTokenExpiredException
import com.konkuk.ma.domain.point.exception.PaymentApprovalFailedException
import com.konkuk.ma.exception.InvalidObfuscatedIdException
import com.konkuk.ma.exception.InvalidValueException
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import com.konkuk.ma.domain.member.exception.WithdrawalPendingLoginException
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.exception.BusinessException
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.logger
import com.konkuk.ma.support.payload.response.ApiError
import com.konkuk.ma.support.payload.response.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.ENTITY_NOT_FOUND)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(DuplicateException::class)
    fun handleDuplicateException(e: DuplicateException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.ENTITY_DUPLICATION)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(PasswordMismatchException::class, RefreshTokenExpiredException::class)
    fun handleUnauthorizedException(e: BusinessException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.UNAUTHORIZED)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(PaymentApprovalFailedException::class)
    fun handlePaymentApprovalFailedException(e: PaymentApprovalFailedException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.PAYMENT_APPROVAL_FAILED)
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.ACCESS_DENIED)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(WithdrawalPendingLoginException::class)
    fun handleWithdrawalPendingLoginException(e: WithdrawalPendingLoginException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.WITHDRAWAL_PENDING)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(
        InvalidValueException::class,
        InvalidStateException::class,
        SmsNotVerifiedException::class,
    )
    fun handleBadRequestException(e: BusinessException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.INVALID_INPUT_VALUE)
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(InvalidObfuscatedIdException::class)
    fun handleInvalidObfuscatedId(e: InvalidObfuscatedIdException): ResponseEntity<ApiError> {
        val error = ApiError(ErrorCode.INVALID_TYPE_VALUE)
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = e.bindingResult.allErrors.mapNotNull { it.defaultMessage }.joinToString(", ")
        val error = ApiError(ErrorCode.INVALID_INPUT_VALUE, message)
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ResponseEntity<ApiError> {
        val maxSize = e.maxUploadSize
        val message = if (maxSize > 0) {
            "파일 크기가 허용 한도(${maxSize / 1024 / 1024}MB)를 초과했습니다."
        } else {
            "파일 크기가 허용 한도를 초과했습니다."
        }
        val error = ApiError(ErrorCode.FILE_SIZE_EXCEEDED, message)
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiError> {
        logger.error(e) { "예상하지 못한 에러가 발생했습니다." }
        val error = ApiError(ErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
