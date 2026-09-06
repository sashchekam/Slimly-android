package com.slimly.mediakit.camera

import com.slimly.core.model.DocumentQuad

/** Решение автоспуска: пора снимать или лист ещё шевелится. */
internal interface AutoShutterDeciding {
    fun shouldShoot(quad: DocumentQuad?, at: Long = System.currentTimeMillis()): Boolean

    fun reset()
}

/**
 * Автоспуск ждёт не кадров, а времени.
 *
 * На iOS отсчёт идёт шестью спокойными кадрами подряд, но там частота потока
 * известна. Здесь кадры разбираются с прореживанием и на разных устройствах
 * приходят по-разному, поэтому спокойствие меряется секундой на часах —
 * ровно тем, что человек и считает паузой.
 *
 * Признака наводки у Android нет, и заменить его нечем: за секунду
 * неподвижного кадра непрерывный автофокус успевает сойтись сам.
 */
internal class AutoShutter(
    private val tolerance: Float = STEADY_TOLERANCE,
    private val steadyMillis: Long = STEADY_MILLIS,
) : AutoShutterDeciding {
    private var previous: DocumentQuad? = null
    private var steadySince: Long = 0

    override fun shouldShoot(quad: DocumentQuad?, at: Long): Boolean {
        val last = previous
        previous = quad

        if (quad == null) {
            reset()
            return false
        }

        // Первый кадр с листом уже считается спокойным: сравнивать его
        // не с чем, а начинать отсчёт с нуля значило бы терять секунду.
        if (last == null || quad.drift(last) > tolerance) {
            steadySince = at
            return false
        }

        val isSteady = at - steadySince >= steadyMillis
        // Сняли — отсчёт начинается заново: следующая страница ещё не легла.
        if (isSteady) reset()
        return isSteady
    }

    override fun reset() {
        previous = null
        steadySince = 0
    }

    private companion object {
        /**
         * Насколько углам позволено дрожать между кадрами, чтобы лист
         * считался неподвижным. Полпроцента кадра — это дрожание руки,
         * а не сдвиг листа.
         */
        const val STEADY_TOLERANCE = 0.005f

        /** Секунда неподвижности: за неё успевают убрать из кадра палец. */
        const val STEADY_MILLIS = 900L
    }
}
