package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.domain.id.ObfuscationType

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DecryptId(val value: ObfuscationType)
