package com.slimly.core.service

import android.content.Context
import android.os.Build
import com.slimly.core.storage.KeyValueStorage
import com.slimly.core.storage.StorageKey
import java.util.Locale
import java.util.UUID

internal val installIdKey = StorageKey.text("slimly.install.id")

/**
 * Паспорт сборки: то, что показывает отладочное меню и что просят приложить
 * к обращению в поддержку.
 *
 * Значения собраны в одну структуру, а не разбросаны по вызовам системы:
 * строку «версия · устройство · система · ID» показывают в двух местах,
 * и собираться она должна одинаково.
 */
data class AppEnvironment(
    val version: String,
    val build: String,
    val device: String,
    val system: String,
    val installId: String,
) {
    /** «1.4.2 (218)» — ровно то, что видно в Google Play и в настройках. */
    val versionText: String
        get() = "$version ($build)"

    /** Одна строка на всё: шапка отладочного меню и хвост письма в поддержку. */
    val summary: String
        get() = listOf(versionText, device, system, "ID $installId").joinToString(separator = " · ")
}

/** Откуда берётся паспорт сборки. */
interface AppEnvironmentProviding {
    suspend fun environment(): AppEnvironment
}

/**
 * Паспорт текущей сборки.
 *
 * Идентификатор установки свой, а не рекламный: он нужен затем, чтобы два
 * обращения из одной установки узнавались как одно, и ни в какую систему
 * слежения не уходит.
 */
class AppEnvironmentProvider(private val context: Context, private val storage: KeyValueStorage) :
    AppEnvironmentProviding {
    override suspend fun environment(): AppEnvironment {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppEnvironment(
            version = info.versionName ?: UNKNOWN,
            build = info.longVersionCode.toString(),
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            system = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            installId = installId(),
        )
    }

    /**
     * Идентификатор короткий и с дефисом: его диктуют по телефону и вбивают
     * руками, а полный UUID для этого не годится.
     */
    private suspend fun installId(): String {
        storage.value(installIdKey)?.let { return it }

        val raw = UUID.randomUUID().toString()
            .replace("-", "")
            .take(RAW_LENGTH)
            .uppercase(Locale.ROOT)
        val generated = "${raw.take(HALF_LENGTH)}-${raw.takeLast(HALF_LENGTH)}"
        storage.set(installIdKey, generated)
        return generated
    }

    private companion object {
        const val UNKNOWN = "—"
        const val RAW_LENGTH = 8
        const val HALF_LENGTH = 4
    }
}
