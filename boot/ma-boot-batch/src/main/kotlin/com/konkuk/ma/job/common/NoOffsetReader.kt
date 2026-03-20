package com.konkuk.ma.job.common

import com.konkuk.ma.domain.common.HasCursorId
import org.springframework.batch.item.ItemReader

abstract class NoOffsetReader<T : HasCursorId<C>, C>(
    private val readSize: Int = 1000,
    private val readFunction: (cursorId: C?, limit: Int) -> List<T>
) : ItemReader<T> {

    private var currentList: List<T> = emptyList()
    private var currentIndex: Int = 0
    private var lastCursorId: C? = null
    private var finished: Boolean = false

    override fun read(): T? {
        if (finished) return null

        if (currentIndex >= currentList.size) {
            currentList = readFunction(lastCursorId, readSize)

            if (currentList.isEmpty()) {
                finished = true
                return null
            }

            currentIndex = 0
            lastCursorId = currentList.last().cursorId
        }

        val item = currentList[currentIndex]
        currentIndex++
        return item
    }
}
