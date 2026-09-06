package com.slimly.mediakit.processing

import java.io.File
import java.util.UUID

/**
 * Рабочая папка в кэше. Здесь результат живёт до сохранения, и здесь же
 * остаётся после — на экране сравнения «до / после».
 */
internal interface WorkFileProviding {
    fun makeFile(name: String): File

    /**
     * Кэш чистим на старте: сохранённое лежит там, где ему положено,
     * а несохранённое человек сохранять не стал — предлагать ему прошлый
     * результат новым запуском было бы странно.
     */
    fun clear()
}

internal class WorkDirectory(private val root: File) : WorkFileProviding {
    /**
     * Готовый файл лежит под своим настоящим именем, а уникальность даёт
     * папка вокруг него: имя файла видно в системной шторке «Поделиться»,
     * и «b7f1-…-4c2a.jpg» там выглядело бы поломкой.
     */
    override fun makeFile(name: String): File {
        val folder = File(root, UUID.randomUUID().toString())
        folder.mkdirs()
        return File(folder, name)
    }

    override fun clear() {
        root.deleteRecursively()
    }
}
