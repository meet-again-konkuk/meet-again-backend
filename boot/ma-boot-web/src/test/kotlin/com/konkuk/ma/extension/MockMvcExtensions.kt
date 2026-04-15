package com.konkuk.ma.extension

import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private fun MockMvc.performDsl(builder: org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder): ResultActionsDsl {
    return ResultActionsDsl(perform(builder), this)
}

fun MockMvc.postJson(
    uri: String,
    vararg urlVariables: Any?,
    setup: JsonRequestBuilder.() -> Unit = {},
): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    if (urlVariables.isNotEmpty()) {
        val requestBuilder = RestDocumentationRequestBuilders.post(uri, *urlVariables)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
        builder.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        if (builder.content != null) requestBuilder.content(builder.content!!)
        return performDsl(requestBuilder)
    }
    return this.post(uri) {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        if (builder.content != null) {
            content = builder.content
        }
    }
}

fun MockMvc.getJson(
    uri: String,
    vararg urlVariables: Any?,
    setup: JsonRequestBuilder.() -> Unit = {},
): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    if (urlVariables.isNotEmpty()) {
        val requestBuilder = RestDocumentationRequestBuilders.get(uri, *urlVariables)
            .accept(MediaType.APPLICATION_JSON)
        builder.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        builder.params.forEach { (name, value) -> requestBuilder.param(name, value) }
        return performDsl(requestBuilder)
    }
    return this.get(uri) {
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        builder.params.forEach { (name, value) -> param(name, value) }
    }
}

fun MockMvc.putJson(
    uri: String,
    vararg urlVariables: Any?,
    setup: JsonRequestBuilder.() -> Unit = {},
): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    if (urlVariables.isNotEmpty()) {
        val requestBuilder = RestDocumentationRequestBuilders.put(uri, *urlVariables)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
        builder.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        if (builder.content != null) requestBuilder.content(builder.content!!)
        return performDsl(requestBuilder)
    }
    return this.put(uri) {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
        if (builder.content != null) {
            content = builder.content
        }
    }
}

fun MockMvc.deleteJson(
    uri: String,
    vararg urlVariables: Any?,
    setup: JsonRequestBuilder.() -> Unit = {},
): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    if (urlVariables.isNotEmpty()) {
        val requestBuilder = RestDocumentationRequestBuilders.delete(uri, *urlVariables)
            .accept(MediaType.APPLICATION_JSON)
        builder.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        return performDsl(requestBuilder)
    }
    return this.delete(uri) {
        accept = MediaType.APPLICATION_JSON
        builder.headers.forEach { (name, value) -> header(name, value) }
    }
}

fun MockMvc.patchJson(
    uri: String,
    vararg urlVariables: Any?,
    setup: JsonRequestBuilder.() -> Unit = {},
): ResultActionsDsl {
    val builder = JsonRequestBuilder().apply(setup)
    if (urlVariables.isNotEmpty()) {
        val requestBuilder = RestDocumentationRequestBuilders.patch(uri, *urlVariables)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
        builder.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        if (builder.content != null) requestBuilder.content(builder.content!!)
        return performDsl(requestBuilder)
    }
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

    fun param(name: String, value: String) {
        params[name] = value
    }
}
