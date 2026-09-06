package com.slimly.mediakit.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.slimly.core.model.MediaKind
import com.slimly.core.service.MediaAccess

/**
 * Разрешения медиатеки и то, что из них следует.
 *
 * Названия разрешений разъезжаются по версиям трижды: до Android 13 доступ
 * был один на всё внешнее хранилище, с 13 он разделился на фото и видео,
 * а с 14 добавился частичный — «пользователь отобрал несколько файлов».
 */
internal object MediaPermissions {
    fun required(kind: MediaKind?): List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> buildList {
            if (kind != MediaKind.VIDEO) add(Manifest.permission.READ_MEDIA_IMAGES)
            if (kind != MediaKind.PHOTO) add(Manifest.permission.READ_MEDIA_VIDEO)
        }

        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * Что спрашивать у системы. С Android 14 к полному доступу добавляется
     * частичный: без него система показала бы только выбор «разрешить всё
     * или отказать», а отобрать несколько файлов было бы нечем.
     */
    fun requested(kind: MediaKind?): List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            required(kind) + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED

        else -> required(kind)
    }

    /**
     * Что система разрешила прямо сейчас.
     *
     * «Ещё не спрашивали» и «отказано» здесь не различаются: узнать это
     * без Activity нельзя, а с ней — незачем, ответ на запрос виден экрану
     * напрямую. Поэтому «ничего не выдано» — это [MediaAccess.NOT_DETERMINED],
     * и в отказ оно превращается только после настоящего отказа на запрос.
     */
    fun access(context: Context, kind: MediaKind?): MediaAccess = when {
        required(kind).all { granted(context, it) } -> MediaAccess.AUTHORIZED

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.LIMITED

        else -> MediaAccess.NOT_DETERMINED
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
