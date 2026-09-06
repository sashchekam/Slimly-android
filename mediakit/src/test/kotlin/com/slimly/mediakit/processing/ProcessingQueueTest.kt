package com.slimly.mediakit.processing

import com.slimly.core.model.CompressionLevel
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessingQueueTest {
    private val broken = item("broken")
    private val fine = item("fine")

    private val messages = FailureMessaging { it.message.orEmpty() }

    @Test
    fun `битый файл не обрывает очередь`() = runTest {
        val queue = ProcessingQueue(
            items = { item, _, _ ->
                if (item.id == broken.id) error("камера отдала половину файла") else result(item)
            },
            messages = messages,
        )

        val outcome = queue.run(request(broken, fine), progress = { _, _ -> })

        assertEquals(listOf(fine.id), outcome.results.map { it.item.id })
        assertEquals(listOf(broken.id), outcome.failures.map { it.item.id })
        assertEquals("камера отдала половину файла", outcome.failures.single().message)
    }

    @Test
    fun `каждый файл отмечается готовым, чем бы ни кончился`() = runTest {
        val reported = mutableListOf<Pair<Int, Double>>()
        val queue = ProcessingQueue(items = { item, _, _ -> result(item) }, messages = messages)

        queue.run(request(broken, fine), progress = { index, fraction -> reported += index to fraction })

        assertEquals(listOf(0 to 1.0, 1 to 1.0), reported)
    }

    private fun request(vararg items: MediaItem) = ProcessingRequest(
        items = items.toList(),
        operation = ProcessingOperation.Compress(CompressionLevel.DEFAULT),
    )

    private fun result(item: MediaItem) = ProcessingResult(
        item = item,
        outputByteCount = item.byteCount / 2,
        outputName = item.fileName,
        destination = OutputDestination.GALLERY,
    )

    private fun item(id: String) = MediaItem(
        id = id,
        kind = MediaKind.PHOTO,
        fileName = "$id.jpg",
        byteCount = 1_000_000,
    )
}
