package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.common.domain.file.VideoFile

interface VideoUploader {
    fun upload(videoFiles: List<VideoFile>): List<String>

    fun upload(videoFile: VideoFile): String
}
