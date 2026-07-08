package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PostImageTable : BaseTable("COMMUNITY_POST_IMAGES", "COMMUNITY_POST_IMAGE_ID") {
    val postId = long("POST_ID").index()
    val storageKey = varchar("STORAGE_KEY", 512)
    val originalFilename = varchar("ORIGINAL_FILENAME", 255)
    val mimeType = varchar("MIME_TYPE", 100)
    val fileSize = long("FILE_SIZE")
    val thumbnailKey = varchar("THUMBNAIL_KEY", 512).nullable()
}
