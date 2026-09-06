package com.slimly.mediakit.processing

import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.ProcessingProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressTrackerTest {
    private val small = item(id = "small", bytes = 1_000_000)
    private val large = item(id = "large", bytes = 9_000_000)

    private var last: ProcessingProgress? = null
    private val tracker = ProgressTracker(listOf(small, large)) { last = it }

    @Test
    fun `доли считаются по весу файлов, а не по их количеству`() {
        tracker.update(index = 0, fraction = 1.0)

        // Лёгкий файл — это десятая часть работы, а не половина.
        assertEquals(0.1, requireNotNull(last).overall, TOLERANCE)
    }

    @Test
    fun `готовая очередь даёт единицу`() {
        tracker.update(index = 0, fraction = 1.0)
        tracker.update(index = 1, fraction = 1.0)

        assertEquals(1.0, requireNotNull(last).overall, TOLERANCE)
    }

    @Test
    fun `доля файла назад не откатывается`() {
        tracker.update(index = 1, fraction = 0.5)
        tracker.update(index = 1, fraction = 0.2)

        assertEquals(0.5, requireNotNull(last).fraction(large), TOLERANCE)
    }

    @Test
    fun `прогноз не выдумывается на первых процентах`() {
        tracker.update(index = 0, fraction = 0.1)

        assertNull(requireNotNull(last).estimatedRemaining)
    }

    private fun item(id: String, bytes: Long) = MediaItem(
        id = id,
        kind = MediaKind.PHOTO,
        fileName = "$id.jpg",
        byteCount = bytes,
    )

    private companion object {
        const val TOLERANCE = 0.0001
    }
}
