package com.konkuk.ma.domain.auth.api.response

import com.konkuk.ma.domain.common.domain.Email

class EmailRecoveryResponse(
    val email: String
) {
    constructor(email: Email) : this(
        email = email.value
    )
}
