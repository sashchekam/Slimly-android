package com.slimly.feature.debug

import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.debug.DebugSettings
import com.slimly.core.remoteconfig.RemoteConfigService
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Одно значение конфига: что пришло с сервера и что лежит поверх. */
data class DebugConfigEntry(
    /** Имя ключа — оно же ключ списка: одинаковых имён в конфиге нет. */
    val name: String,
    val remoteText: String?,
    val overrideText: String?,
) {
    val isOverridden: Boolean
        get() = overrideText != null

    /**
     * Действующее значение — то, что получит владелец ключа. Прочерк значит
     * «источник промолчал»: в дело пойдёт умолчание самого ключа, а его
     * знает только объявивший ключ код.
     */
    val value: String
        get() = overrideText ?: remoteText ?: DASH

    private companion object {
        const val DASH = "—"
    }
}

/**
 * Что удалённый конфиг отдаёт прямо сейчас и чем это подменено на устройстве.
 *
 * Источник приходит без подмен поверх: экран показывает, что реально приехало
 * с сервера, и подмену рядом с ним, — иначе подменённый ключ выглядел бы как
 * пришедший.
 */
class DebugRemoteConfigViewModel(
    private val settings: DebugSettings,
    private val remoteConfig: RemoteConfigService,
    private val resources: Resources,
    private val router: Router,
    private val scope: CoroutineScope,
) {
    /**
     * Список собирается разово и лежит полем: значения источника — обычный
     * объект, а не состояние Compose, и сами о своём обновлении экрану
     * не сообщат.
     */
    var entries: List<DebugConfigEntry> by mutableStateOf(emptyList())
        private set

    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    var toast: String? by mutableStateOf(null)
        private set

    /**
     * Ключ, который правят прямо сейчас. `null` — правка закрыта.
     * Значение хранит сам список, поэтому здесь достаточно имени.
     */
    private var editedName: String? by mutableStateOf(null)

    /** Набранное в поле. Обычный `var`: это ввод человека, а не состояние. */
    var draft: String by mutableStateOf("")

    private var toastJob: Job? = null

    init {
        reload()
    }

    private fun reload() {
        val overrides = settings.remoteConfigOverrides
        entries = (remoteConfig.names + overrides.names).distinct().sorted().map { name ->
            DebugConfigEntry(name = name, remoteText = remoteConfig.text(name), overrideText = overrides[name])
        }
    }

    val hasOverrides: Boolean
        get() = !settings.remoteConfigOverrides.isEmpty

    /** Откуда взялось действующее значение — строкой под именем ключа. */
    fun source(entry: DebugConfigEntry): String = when {
        entry.isOverridden && entry.remoteText != null ->
            resources.getString(R.string.config_state_overridden, entry.remoteText)

        entry.isOverridden -> resources.getString(R.string.config_state_overridden_only)

        entry.remoteText != null -> resources.getString(R.string.config_state_remote)

        else -> resources.getString(R.string.config_state_default)
    }

    // MARK: Правка

    val isEditing: Boolean
        get() = editedName != null

    val editedTitle: String
        get() = editedName.orEmpty()

    /** У неподменённого ключа снимать нечего — кнопки в диалоге быть не должно. */
    val canRestore: Boolean
        get() = editedName?.let { settings.remoteConfigOverrides[it] != null } == true

    fun edit(entry: DebugConfigEntry) {
        editedName = entry.name
        draft = entry.overrideText ?: entry.remoteText.orEmpty()
    }

    /** Закрыть правку могут и мимо кнопки — нажатием по затемнению. */
    fun cancelEdit() {
        editedName = null
    }

    fun applyEdit() {
        val name = editedName ?: return
        editedName = null

        scope.launch {
            settings.setRemoteConfigOverride(draft, name)
            val cleared = settings.remoteConfigOverrides[name] == null
            reload()
            say(if (cleared) R.string.config_override_cleared else R.string.config_override_set)
        }
    }

    fun restore() {
        val name = editedName ?: return
        editedName = null

        scope.launch {
            settings.setRemoteConfigOverride(null, name)
            reload()
            say(R.string.config_override_cleared)
        }
    }

    fun clearAll() {
        scope.launch {
            settings.clearRemoteConfigOverrides()
            reload()
            say(R.string.config_all_cleared)
        }
    }

    /**
     * Значения тянутся заново прямо с сервера: в отладочной сборке кэш
     * у SDK отключён, поэтому правку в веб-консоли видно сразу.
     */
    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true

        scope.launch {
            remoteConfig.refresh()
            isRefreshing = false
            reload()
            say(if (entries.isEmpty()) R.string.config_nothing_fetched else R.string.config_updated)
        }
    }

    fun goBack() {
        router.pop()
    }

    private fun say(text: Int) {
        toastJob?.cancel()
        toast = resources.getString(text)
        toastJob = scope.launch {
            delay(TOAST_MILLIS)
            toast = null
        }
    }

    private companion object {
        const val TOAST_MILLIS = 2600L
    }
}
