package com.konkuk.ma.job.common

import com.konkuk.ma.domain.common.HasCursorId
import org.springframework.batch.item.ItemReader

abstract class NoOffsetListReader<T : HasCursorId<C>, C>(
    private val readSize: Int = 1000,
    private val readFunction: (cursorId: C?, limit: Int) -> List<T>
) : ItemReader<List<T>> {

    private var lastCursorId: C? = null
    private var finished: Boolean = false

    override fun read(): List<T>? {
        if (finished) return null

        val items = readFunction(lastCursorId, readSize)

        if (items.isEmpty()) {
            finished = true
            return null
        }

        lastCursorId = items.last().cursorId
        return items
    }
}
