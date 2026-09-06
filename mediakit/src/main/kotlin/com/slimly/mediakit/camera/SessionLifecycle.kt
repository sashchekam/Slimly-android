package com.slimly.mediakit.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Свой жизненный цикл для камеры.
 *
 * CameraX привязывает съёмку к чужому циклу и сам гасит её вместе с экраном.
 * Отдать ему цикл Activity нельзя: тогда контракт сессии знал бы про
 * `LifecycleOwner`, а он не должен — сессию заказывает экран и сам говорит,
 * когда пустить кадр и когда погасить. Поэтому цикл здесь собственный,
 * а совпадать с экранным его заставляет тот, кто зовёт `start` и `stop`.
 */
internal class SessionLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    /** Кадр идёт: камера привязывается и включается. */
    fun start() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    /** Камера гаснет, но привязка остаётся — следующий `start` дешевле. */
    fun stop() {
        registry.currentState = Lifecycle.State.CREATED
    }
}
