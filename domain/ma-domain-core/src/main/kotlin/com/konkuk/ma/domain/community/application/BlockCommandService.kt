package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.block.BlockRegistrar
import com.konkuk.ma.domain.community.domain.block.BlockResult
import com.konkuk.ma.domain.community.domain.block.NewBlock
import com.konkuk.ma.domain.community.domain.port.BlockCommandRepository
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BlockCommandService(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val blockCommandRepository: BlockCommandRepository,
    private val blockQueryRepository: BlockQueryRepository,
    private val blockRegistrar: BlockRegistrar,
) {
    fun blockPostAuthor(postId: Long, blockerId: Long): BlockResult {
        val post = postQueryRepository.findOne(postId)
        return blockRegistrar.register(NewBlock(blockerId, post.authorId))
    }

    fun blockCommentAuthor(commentId: Long, blockerId: Long): BlockResult {
        val comment = commentQueryRepository.findOne(commentId)
        return blockRegistrar.register(NewBlock(blockerId, comment.authorId))
    }

    fun unblock(blockId: Long, blockerId: Long) {
        val block = blockQueryRepository.findOne(blockId)
        block.validateOwnership(blockerId)
        blockCommandRepository.softDelete(blockId, blockerId)
    }
}
