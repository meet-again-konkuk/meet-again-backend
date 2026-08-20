package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.FileUrls
import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import org.springframework.stereotype.Component

@Component
class MemberPhotoUrlResolver(
    private val fileUrlResolver: FileUrlResolver,
) {
    fun resolve(photo: MemberPhoto): String {
        return fileUrlResolver.resolve(photo.pickImageKey())
    }

    fun resolveByMember(photos: List<MemberPhoto>): FileUrls {
        return FileUrls(photos.associate { it.memberId to resolve(it) })
    }
}
