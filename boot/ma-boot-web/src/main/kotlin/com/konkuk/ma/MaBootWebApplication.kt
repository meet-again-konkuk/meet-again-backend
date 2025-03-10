package com.konkuk.ma

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MaBootWebApplication

fun main(args: Array<String>) {
	runApplication<MaBootWebApplication>(*args)
}
