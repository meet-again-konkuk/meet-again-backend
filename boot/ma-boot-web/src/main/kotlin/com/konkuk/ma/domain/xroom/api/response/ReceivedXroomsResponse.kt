package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.xroom.domain.ReceivedXrooms

class ReceivedXroomsResponse(
    val rooms: List<ReceivedXroomResponse>,
) {
    companion object {
        fun from(receivedXrooms: ReceivedXrooms): ReceivedXroomsResponse {
            return ReceivedXroomsResponse(
                rooms = receivedXrooms.data.map { ReceivedXroomResponse.from(it) }
            )
        }
    }
}
