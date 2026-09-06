package com.slimly.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика Nocturne.
 *
 * Система построена на Inter; файла шрифта в проекте нет, поэтому берём
 * системный гротеск — он ближе к Inter по метрикам, чем скруглённый.
 * Иерархия здесь держится на размере и воздухе, а не на жирности: заголовки
 * не тяжелее [FontWeight.Medium].
 *
 * Трекинг вписан в сам стиль, а не вынесен таблицей, как на iOS: там он
 * задаётся отдельным модификатором, здесь — полем [TextStyle].
 */
object SlimlyText {
    // Заголовки экранов

    val LargeTitle = TextStyle(fontSize = 33.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.7).sp)
    val Title = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp)

    // Цифры-показатели

    val StatXL = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.2).sp)
    val StatL = TextStyle(fontSize = 33.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.1).sp)
    val StatM = TextStyle(fontSize = 31.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp)
    val StatS = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.6).sp)
    val StatRing = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium)

    /** Цифра на плитке инструмента: карточная, но мельче показателя экрана. */
    val StatTile = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp)

    /** Цифра в шапке карточки инструмента на экране статистики. */
    val StatCard = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp)

    /** Зачёркнутый размер «до» рядом со стрелкой. */
    val StatStruck = TextStyle(fontSize = 20.sp)

    // Текст

    val SectionTitle = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
    val RowTitle = TextStyle(fontSize = 13.5.sp)
    val Body = TextStyle(fontSize = 13.sp)
    val BodySmall = TextStyle(fontSize = 12.5.sp)
    val Caption = TextStyle(fontSize = 11.5.sp)
    val CaptionSmall = TextStyle(fontSize = 11.sp)
    val Micro = TextStyle(fontSize = 10.5.sp)
    val Nano = TextStyle(fontSize = 9.5.sp)

    /**
     * Надпись-кикер над блоком: «ОСВОБОЖДЕНО ВСЕГО», «БУДЕТ», «ГОТОВО».
     * Разряжённая и в верхнем регистре — регистр ставит место показа.
     */
    val Kicker = TextStyle(fontSize = 10.5.sp, letterSpacing = 1.365.sp)

    // Управление

    val Button = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
    val ButtonSmall = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    val TabLabel = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
    val Pill = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)

    /** Надпись на чипе выбора — крупнее пилюли: по ней выбирают, а не читают. */
    val ChipLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)

    /** Значение формата в паре «из → в». */
    val FormatValue = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium)

    // Служебное: живёт только в отладочном меню

    /**
     * Моноширинный текст сервисных экранов. Имена ключей конфига и событий
     * сверяют с реестром посимвольно, и подчёркнутая служебность тут
     * уместнее ровного текста.
     */
    val Mono = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)

    val MonoSmall = TextStyle(fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)

    /** Метка сборки: «INTERNAL», «DEBUG». */
    val DebugTag = TextStyle(
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.95.sp,
    )
}
