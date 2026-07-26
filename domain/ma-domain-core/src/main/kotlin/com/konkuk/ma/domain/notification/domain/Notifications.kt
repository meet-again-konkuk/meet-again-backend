package com.konkuk.ma.domain.notification.domain

import com.konkuk.ma.domain.member.domain.Members

class Notifications(val data: List<Notification>) {

    fun extractActorIds(): Set<Long> = data.map { it.actorId }.toSet()

    fun combineWithActors(actors: Members): List<NotificationWithActor> {
        return data.map { it.withActor(actors) }
    }
}
