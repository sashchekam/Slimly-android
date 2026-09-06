package com.slimly.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val STORE_NAME = "slimly"

private val Context.slimlyPreferences: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

/**
 * Хранилище поверх DataStore — то, что стоит в приложении.
 *
 * В отличие от `UserDefaults`, DataStore помнит тип записи, поэтому гадать
 * при чтении, чем было число, не приходится: примитив узнаётся прямо
 * из хранилища.
 */
class DataStoreStorage(private val store: DataStore<Preferences>) : KeyValueStorage {
    constructor(context: Context) : this(context.applicationContext.slimlyPreferences)

    override fun <T : Any> observe(key: StorageKey<T>): Flow<T?> =
        preferences.map { it.decode(key) }.distinctUntilChanged()

    override suspend fun <T : Any> value(key: StorageKey<T>): T? = preferences.first().decode(key)

    override suspend fun <T : Any> set(key: StorageKey<T>, value: T) {
        store.edit { it.write(key.name, key.codec.encode(value)) }
    }

    override suspend fun <T : Any> remove(key: StorageKey<T>) {
        store.edit { preferences ->
            preferences.asMap().keys
                .filter { it.name == key.name }
                .forEach { preferences.remove(it) }
        }
    }

    /**
     * Сорванное чтение файла — это «значений нет», а не ошибка экрана:
     * настройки читают отовсюду, и падать на них приложение не должно.
     */
    private val preferences: Flow<Preferences>
        get() = store.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
}

private fun <T : Any> Preferences.decode(key: StorageKey<T>): T? = stored(key.name)?.let(key.codec::decode)

private fun Preferences.stored(name: String): StoredValue? {
    val raw = asMap().entries.firstOrNull { it.key.name == name }?.value
    return when (raw) {
        is Boolean -> StoredValue.Flag(raw)
        is Int -> StoredValue.Whole(raw)
        is Double -> StoredValue.Decimal(raw)
        is String -> StoredValue.Text(raw)
        else -> null
    }
}

private fun MutablePreferences.write(name: String, stored: StoredValue) {
    when (stored) {
        is StoredValue.Flag -> set(booleanPreferencesKey(name), stored.value)
        is StoredValue.Whole -> set(intPreferencesKey(name), stored.value)
        is StoredValue.Decimal -> set(doublePreferencesKey(name), stored.value)
        is StoredValue.Text -> set(stringPreferencesKey(name), stored.value)
    }
}
