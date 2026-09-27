package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.xroom.domain.MyXrooms

class MyXroomsResponse(
    val rooms: List<MyXroomResponse>,
) {
    companion object {
        fun from(myXrooms: MyXrooms): MyXroomsResponse {
            return MyXroomsResponse(
                rooms = myXrooms.data.map { MyXroomResponse.from(it) }
            )
        }
    }
}
