package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member

class Targets(
    val data: List<Target>
) {
    fun filterCandidates(name: String, gender: Gender): List<Target> {
        return data.filter { it.matchesNameAndGender(name, gender) }
    }

    companion object {
        fun from(members: List<Member>): Targets {
            return Targets(members.map { Target.create(it) })
        }
    }
}
