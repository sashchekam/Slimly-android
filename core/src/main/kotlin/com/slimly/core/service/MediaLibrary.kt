package com.slimly.core.service

import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import com.slimly.core.R
import com.slimly.core.model.LibrarySnapshot
import com.slimly.core.model.MediaAlbum
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.MediaPage
/** Что система разрешила делать с медиатекой. */
enum class MediaAccess {
    NOT_DETERMINED,
    DENIED,

    /**
     * Пользователь выдал доступ к части файлов — работать можно, но
     * показываем мы только отобранное им.
     */
    LIMITED,
    AUTHORIZED,
    ;

    val allowsBrowsing: Boolean
        get() = this == AUTHORIZED || this == LIMITED
}

/**
 * Отказ медиатеки.
 *
 * Текст для человека приходит либо ключом строки — у наших отказов, — либо
 * сообщением системы: причину экспорта мы не переводим, её пишет платформа.
 */
sealed class MediaLibraryError(@get:StringRes val messageRes: Int?, message: String? = null) : Exception(message) {
    class AccessDenied : MediaLibraryError(R.string.library_error_access_denied)

    class ItemNotFound : MediaLibraryError(R.string.library_error_item_not_found)

    /** Оригинал лежит в облаке, а не на устройстве. */
    class OriginalUnavailable : MediaLibraryError(R.string.library_error_original_unavailable)

    class ExportFailed(reason: String) : MediaLibraryError(messageRes = null, message = reason)
}

/**
 * Доступ к медиатеке устройства.
 *
 * Здесь и просмотр, и выдача оригиналов обработчику: и то и другое — работа
 * с одними и теми же записями, и разделять их значило бы дважды спрашивать
 * систему об одном и том же. Общего веса медиатеки система не сообщает,
 * поэтому такого метода нет и быть не может.
 * Вид файлов задаёт инструмент: конвертеру видео нечего показывать фото.
 */
interface MediaLibrary {
    /**
     * Разрешения, без которых витрина пуста.
     *
     * Иосного `requestAccess()` здесь нет: системный запрос показывает
     * Activity, а сервис живёт на уровне приложения и Activity не видит.
     * Поэтому сервис только называет разрешения, а показывает запрос экран,
     * которому доступ понадобился.
     */
    fun permissions(kind: MediaKind?): List<String>

    suspend fun access(): MediaAccess

    suspend fun snapshot(): LibrarySnapshot

    suspend fun albums(kind: MediaKind?): List<MediaAlbum>

    /**
     * Страница файлов альбома. Медиатека может быть на десятки тысяч файлов,
     * поэтому витрина читается окнами: экран открывается сразу, а остальное
     * догружается по мере прокрутки.
     */
    suspend fun items(albumId: String, kind: MediaKind?, offset: Int, limit: Int): MediaPage

    /**
     * Миниатюра файла. Одна, а не поток «грубый кадр, следом чёткий», как
     * на iOS: MediaStore отдаёт готовую миниатюру сразу и промежуточной
     * ступени у него нет.
     *
     * Растр, а не байты: плитка рисует его как есть, и кодировать кадр
     * в JPEG ради того, чтобы тут же его разобрать, — работа впустую.
     */
    suspend fun thumbnail(item: MediaItem, size: Int): Bitmap?

    /**
     * Копия оригинала во временной папке — источник для обработки.
     * Удалять её должен тот, кто заказал: система за нами не убирает.
     */
    suspend fun exportOriginal(item: MediaItem): Uri

    /**
     * Ссылка на оригинал для воспроизведения.
     *
     * В отличие от [exportOriginal] файл не копируется: плееру хватает того,
     * что уже лежит в медиатеке. `null` — если играть нечего.
     */
    suspend fun playbackUri(item: MediaItem): Uri?

    /**
     * Положить готовый файл в медиатеку. Возвращает адрес созданной записи.
     *
     * @param source элемент, из которого файл получен. Его дата съёмки
     *   переезжает на результат: иначе перекодированное видео встаёт в конец
     *   галереи сегодняшним числом, а не рядом с оригиналом. `null` — у файла
     *   нет прошлого: страницу с камеры сняли только что.
     */
    suspend fun saveToLibrary(file: Uri, kind: MediaKind, source: MediaItem?): Uri

    /**
     * Запрос на удаление оригиналов. Система показывает собственное
     * подтверждение, поэтому своего мы не рисуем — но показать системное
     * может только Activity, поэтому сервис отдаёт намерение, а запускает
     * его экран. `null` — спрашивать не о чем: файлы уже удалены.
     */
    suspend fun deleteRequest(items: List<MediaItem>): IntentSender?
}
