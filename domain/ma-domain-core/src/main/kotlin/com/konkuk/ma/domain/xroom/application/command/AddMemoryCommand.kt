package com.konkuk.ma.domain.xroom.application.command

data class AddMemoryCommand(
    val title: String,
    val eventDate: String,
    val eventDatePrecision: String,
    val location: String?,
    val emotionTags: List<String>,
    val text: String?,
    val letter: String?,
)
