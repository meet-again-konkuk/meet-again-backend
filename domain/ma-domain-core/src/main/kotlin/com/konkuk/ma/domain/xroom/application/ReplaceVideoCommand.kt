package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.file.VideoFile

class ReplaceVideoCommand(
    val blockId: Long,
    val videoId: Long,
    val email: String,
    val videoFile: VideoFile,
)
