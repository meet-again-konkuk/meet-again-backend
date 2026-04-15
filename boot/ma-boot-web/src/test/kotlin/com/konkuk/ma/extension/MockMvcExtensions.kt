package com.konkuk.ma.extension

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

fun MockMvc.postJson(uri: String, setup: JsonRequestBuilder.() -> Unit): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    return this.post(uri) {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        if (builder.content != null) {
            content = builder.content
        }
    }
}

fun MockMvc.getJson(uri: String, setup: JsonRequestBuilder.() -> Unit): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    return this.get(uri) {
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        builder.params.forEach { (name, value) -> param(name, value) }
    }
}


fun MockMvc.putJson(uri: String, setup: JsonRequestBuilder.() -> Unit): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    return this.put(uri) {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        if (builder.content != null) {
            content = builder.content
        }
    }
}

fun MockMvc.patchJson(uri: String, setup: JsonRequestBuilder.() -> Unit): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    return this.patch(uri) {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        if (builder.content != null) {
            content = builder.content
        }
    }
}

class JsonRequestBuilder {
    var content: String? = null
    val headers = mutableMapOf<String, String>()
    val params = mutableMapOf<String, String>()

    fun header(name: String, value: String) {
        headers[name] = value
    }

    fun authorization(token: String) {
        headers["Authorization"] = token
    }

    fun param(name: String, value: String) { // 추가
        params[name] = value
    }
}
