package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.community.api.response.PostImageUploadResponse
import com.konkuk.ma.domain.community.application.PostImageCommandService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/community/posts/{postId}/image")
class PostImageApi(
    private val postImageCommandService: PostImageCommandService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
        @RequestPart("image") image: MultipartFile,
    ): ResponseEntity<PostImageUploadResponse> {
        val photoFile = PhotoFile.create(image.originalFilename, image.size, image.bytes)
        val uploadResult = postImageCommandService.upload(postId, memberInfo.id, photoFile)
        val status = if (uploadResult.replaced) HttpStatus.OK else HttpStatus.CREATED
        return ResponseEntity.status(status).body(PostImageUploadResponse.from(uploadResult))
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
    ) {
        postImageCommandService.delete(postId, memberInfo.id)
    }
}
