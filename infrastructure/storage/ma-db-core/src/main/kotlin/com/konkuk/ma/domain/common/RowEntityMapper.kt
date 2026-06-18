package com.konkuk.ma.domain.common

import com.konkuk.ma.domain.auth.entity.RefreshTokenEntity
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.matching.entity.TargetInfoEntity
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.point.entity.PointProductEntity
import com.konkuk.ma.domain.point.entity.table.PointProductTable
import org.jetbrains.exposed.sql.ResultRow

object RowEntityMapper {
    fun toMemberEntity(row: ResultRow) = MemberEntity(
        id = row[MemberTable.id].value,
        email = row[MemberTable.email],
        password = row[MemberTable.password],
        nickname = row[MemberTable.nickname],
        gender = enumValueOf(row[MemberTable.gender]),
        phoneNumber = row[MemberTable.phoneNumber],
        name = row[MemberTable.name],
        region = enumValueOf(row[MemberTable.region]),
        birthDate = row[MemberTable.birthDate],
        highSchool = row[MemberTable.highSchool],
        university = row[MemberTable.university],
        withdrawalRequestedAt = row[MemberTable.withdrawalRequestedAt]
    )

    fun toRefreshTokenEntity(row: ResultRow) = RefreshTokenEntity(
        memberId = row[RefreshTokenTable.memberId],
        expirationDate = row[RefreshTokenTable.expirationDate],
        token = row[RefreshTokenTable.token],
    )

    fun toTargetInfoEntity(row: ResultRow) = TargetInfoEntity(
        id = row[TargetInfoTable.id].value,
        registerEmail = row[TargetInfoTable.registerEmail],
        name = row[TargetInfoTable.name],
        targetGender = Gender.valueOf(row[TargetInfoTable.targetGender]),
        middleNumber = row[TargetInfoTable.middleNumber],
        lastNumber = row[TargetInfoTable.lastNumber],
        year = row[TargetInfoTable.year],
        month = row[TargetInfoTable.month],
        day = row[TargetInfoTable.day],
        region = row[TargetInfoTable.region]?.let { Region.valueOf(it) },
        createdDate = row[TargetInfoTable.createdDate],
        lastModifiedDate = row[TargetInfoTable.lastModifiedDate]
    )

    fun toPointProductEntity(row: ResultRow) = PointProductEntity(
        id = row[PointProductTable.id].value,
        name = row[PointProductTable.name],
        quantity = row[PointProductTable.quantity],
        price = row[PointProductTable.price],
        displayOrder = row[PointProductTable.displayOrder],
        discountPolicyId = row[PointProductTable.discountPolicyId],
    )
}
