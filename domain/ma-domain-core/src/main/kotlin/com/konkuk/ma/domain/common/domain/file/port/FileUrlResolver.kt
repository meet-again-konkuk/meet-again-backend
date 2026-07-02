package com.konkuk.ma.domain.common.domain.file.port

interface FileUrlResolver {
    fun resolve(storageKey: String): String
}
