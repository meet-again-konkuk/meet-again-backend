package com.konkuk.ma.support.error

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity.badRequest().body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<String> {
        val message = e.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "Invalid Request"
        return ResponseEntity.badRequest().body(message)
    }
}
