package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.TargetInfos

class Xrooms(
    val data: List<Xroom>,
) {
    fun toMine(targetInfos: TargetInfos): MyXrooms {
        return MyXrooms(
            data.map { xroom ->
                MyXroom(
                    xroom = xroom,
                    recipientName = targetInfos.findName(xroom.targetInfoId),
                    // 기억(Memory)은 Phase 2에서 도입 — 현재 방의 기억 수는 항상 0
                    memoryCount = 0,
                )
            }
        )
    }
}
