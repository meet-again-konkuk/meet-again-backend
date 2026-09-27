package com.konkuk.ma.domain.xroom.domain

enum class XroomTemplate(val wireValue: String) {
    CHAT_MEMORY("chat_memory"),
    ;

    companion object {
        val DEFAULT = CHAT_MEMORY

        fun from(wireValue: String): XroomTemplate {
            return entries.first { it.wireValue == wireValue }
        }
    }
}
