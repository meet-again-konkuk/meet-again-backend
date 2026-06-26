package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.TargetInfos

class Xrooms(
    val data: List<Xroom>,
) {
    fun toMine(targetInfos: TargetInfos, memoryCounts: Map<Long, Int>): MyXrooms {
        return MyXrooms(
            data.map { xroom ->
                MyXroom(
                    xroom = xroom,
                    recipientName = targetInfos.findName(xroom.targetInfoId),
                    memoryCount = memoryCounts[xroom.id] ?: 0,
                )
            }
        )
    }
}
