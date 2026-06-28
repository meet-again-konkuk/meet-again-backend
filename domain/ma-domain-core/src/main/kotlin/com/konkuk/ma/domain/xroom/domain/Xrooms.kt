package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.xroom.domain.memory.MemoryCounts

class Xrooms(
    val data: List<Xroom>,
) {
    fun extractOwnerIds(): Set<Long> = data.map { it.ownerId }.toSet()

    fun extractIds(): Set<Long> = data.map { it.id }.toSet()

    fun toMine(targetInfos: TargetInfos, memoryCounts: MemoryCounts): MyXrooms {
        return MyXrooms(
            data.map { xroom ->
                MyXroom(
                    xroom = xroom,
                    recipientName = targetInfos.findName(xroom.targetInfoId),
                    memoryCount = memoryCounts.countOf(xroom.id),
                )
            }
        )
    }

    fun toReceived(senders: Members, memoryCounts: MemoryCounts): ReceivedXrooms {
        return ReceivedXrooms(
            data.map { xroom ->
                ReceivedXroom(
                    xroom = xroom,
                    senderName = senders.findName(xroom.ownerId),
                    memoryCount = memoryCounts.countOf(xroom.id),
                )
            }
        )
    }
}
