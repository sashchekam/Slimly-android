package com.slimly.core.service

import com.slimly.core.storage.KeyValueStorage
import com.slimly.core.storage.StorageKey
import com.slimly.core.storage.value

internal val onboardingTourSeenKey = StorageKey.flag("slimly.onboarding.tourSeen")

/**
 * Показывали ли уже вводный тур.
 *
 * Решение «с чего начать приложение» принимает старт, а отметку о показе
 * ставит последний шаг тура — поэтому флаг вынесен из обоих и живёт
 * за интерфейсом: подставив другую реализацию, тур можно гонять сколько
 * угодно раз, не трогая хранилище устройства.
 */
interface OnboardingState {
    suspend fun hasSeenTour(): Boolean

    /**
     * Отметку умеет и снимать: отладочному меню нужно вернуть сборку
     * в состояние первого запуска, а не только отметить показ.
     */
    suspend fun setTourSeen(seen: Boolean)
}

suspend fun OnboardingState.markTourSeen() {
    setTourSeen(true)
}

/** Отметка в хранилище: тур показывается один раз за установку. */
class StoredOnboardingState(private val storage: KeyValueStorage) : OnboardingState {
    override suspend fun hasSeenTour(): Boolean = storage.value(onboardingTourSeenKey, false)

    override suspend fun setTourSeen(seen: Boolean) {
        storage.set(onboardingTourSeenKey, seen)
    }
}
