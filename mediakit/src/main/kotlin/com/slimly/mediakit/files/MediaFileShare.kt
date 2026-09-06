package com.slimly.mediakit.files

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.slimly.core.service.MediaFileSharing
import java.io.File

/**
 * Готовый файл наружу через `FileProvider`.
 *
 * Наружу уходит не адрес записи медиатеки и не путь к файлу, а `content://`
 * нашего провайдера: путь чужое приложение прочитать не может вовсе,
 * а запись медиатеки за пределами галереи ничего не значит — и права на неё
 * мы выдать не вправе.
 *
 * Отсюда же следует, почему делимся копией из кэша, а не сохранённым файлом:
 * копия остаётся у нас и после сохранения, и поделиться результатом можно
 * до того, как человек решил, куда его класть.
 */
class MediaFileShare(private val context: Context) : MediaFileSharing {
    override fun shareIntent(files: List<Uri>): Intent? {
        val shared = files.mapNotNull { content(it) }
        if (shared.isEmpty()) return null

        val intent = if (shared.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, shared.single().uri)
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shared.map { it.uri }))
        }

        return Intent.createChooser(intent.apply { type = commonType(shared) }.granting(shared), null)
    }

    override fun viewIntent(file: Uri): Intent? {
        val shared = content(file) ?: return null
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(shared.uri, shared.type)
            .granting(listOf(shared))

        // Кнопку без адресата не показываем: «Открыть в…», которое отвечает
        // «нет приложения», — хуже отсутствующей кнопки. Видимость чужих
        // приложений объявлена в манифесте `:mediakit`.
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    /**
     * Право на чтение выдаётся и флагом, и `ClipData`: без второго системная
     * шторка теряет его по дороге к выбранному приложению — намерение
     * переезжает туда через чужую Activity.
     */
    private fun Intent.granting(files: List<SharedFile>): Intent = apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, files.first().name, files.first().uri).also { clip ->
            files.drop(1).forEach { clip.addItem(ClipData.Item(it.uri)) }
        }
    }

    /**
     * Тип пачки: точный, пока файлы одного рода, и «что угодно», когда они
     * разошлись. Врать нельзя — приложения выбирают себя по типу.
     */
    private fun commonType(files: List<SharedFile>): String =
        files.map { it.type }.distinct().singleOrNull() ?: ANY_TYPE

    /**
     * Адрес готового файла у провайдера. `null` — файл лежит вне рабочей
     * папки: провайдер отдаёт только её, и это не поломка, а его работа.
     */
    private fun content(file: Uri): SharedFile? {
        val path = file.path ?: return null
        val target = File(path)
        val uri = runCatching { FileProvider.getUriForFile(context, authority, target) }.getOrNull() ?: return null
        return SharedFile(uri = uri, name = target.name, type = mimeType(target.name) ?: OCTET_STREAM)
    }

    /** Совпадает с `android:authorities` провайдера в манифесте `:mediakit`. */
    private val authority: String
        get() = "${context.packageName}.files"

    private data class SharedFile(val uri: Uri, val name: String, val type: String)

    private companion object {
        const val ANY_TYPE = "*/*"
    }
}
