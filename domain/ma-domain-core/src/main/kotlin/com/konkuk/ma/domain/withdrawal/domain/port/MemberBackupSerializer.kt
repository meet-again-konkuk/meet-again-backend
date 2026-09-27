package com.konkuk.ma.domain.withdrawal.domain.port

import com.konkuk.ma.domain.withdrawal.domain.MemberWithdrawalBackup

interface MemberBackupSerializer {
    fun serialize(backup: MemberWithdrawalBackup): ByteArray
}
