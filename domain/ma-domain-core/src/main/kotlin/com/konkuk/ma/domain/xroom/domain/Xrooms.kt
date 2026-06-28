package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.member.domain.Members

class Xrooms(
    val data: List<Xroom>,
) {
    fun extractOwnerIds(): Set<Long> = data.map { it.ownerId }.toSet()

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

    fun toReceived(senders: Members): ReceivedXrooms {
        return ReceivedXrooms(
            data.map { xroom ->
                ReceivedXroom(
                    xroom = xroom,
                    senderName = senders.findName(xroom.ownerId),
                    // 기억(Memory)은 Phase 2에서 도입 — 현재 방의 기억 수는 항상 0
                    memoryCount = 0,
                )
            }
        )
    }
}
