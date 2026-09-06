package com.slimly.feature.onboarding

import androidx.annotation.StringRes

/**
 * Шаг вводного тура: кикер, заголовок, текст и иллюстрация к ним.
 *
 * Шаги — это содержание самого тура, а не состав приложения: они ничего
 * не открывают и ни на какую фичу не ссылаются, поэтому лежат здесь,
 * а не приходят из `:app`, как плитки главного экрана.
 */
data class OnboardingPage(
    val illustration: Illustration,
    @get:StringRes val kicker: Int,
    @get:StringRes val title: Int,
    @get:StringRes val text: Int,
) {
    /**
     * Что показывает картинка над текстом. Иллюстрации разные не только
     * содержанием, но и раскладкой, поэтому это тип, а не имя ресурса.
     */
    enum class Illustration {
        COMPRESS,
        CONVERT,
        AUDIO,
        SCAN,
    }

    companion object {
        /**
         * Порядок шагов повторяет порядок инструментов на главной: первым идёт
         * то, ради чего приложение ставят, последним — самый узкий сценарий.
         *
         * Звук и скан здесь есть, хотя инструментов этих в сборке пока нет:
         * тур обещает приложение целиком, и вырезать из него половину значило
         * бы переписать обещание под порядок фаз.
         */
        val TOUR = listOf(
            OnboardingPage(
                illustration = Illustration.COMPRESS,
                kicker = R.string.compress_kicker,
                title = R.string.compress_title,
                text = R.string.compress_text,
            ),
            OnboardingPage(
                illustration = Illustration.CONVERT,
                kicker = R.string.convert_kicker,
                title = R.string.convert_title,
                text = R.string.convert_text,
            ),
            OnboardingPage(
                illustration = Illustration.AUDIO,
                kicker = R.string.audio_kicker,
                title = R.string.audio_title,
                text = R.string.audio_text,
            ),
            OnboardingPage(
                illustration = Illustration.SCAN,
                kicker = R.string.scan_kicker,
                title = R.string.scan_title,
                text = R.string.scan_text,
            ),
        )
    }
}
