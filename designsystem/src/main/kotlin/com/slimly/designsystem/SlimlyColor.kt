package com.slimly.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Палитра Nocturne: тёмная основа, один блюрпл-акцент, тональные рампы 100–900.
 *
 * Рампы построены в OKLCH по общей шкале светлоты, поэтому одинаковый шаг
 * любой роли весит визуально одинаково. Контраст здесь берётся из рамп,
 * а не из насыщенности — акцентом красим линии и метки, но не заливаем площади.
 *
 * Числа скопированы из `Colors.swift` на iOS: палитра у платформ общая,
 * и расходиться ей нельзя.
 */
object SlimlyColor {
    // Роли

    val Background = Color(0xFF161826)
    val Surface = Color(0xFF232532)
    val Text = Color(0xFFE9E9ED)
    val Accent = Color(0xFF9184D9)
    val Divider = Text.copy(alpha = 0.16f)

    // Нейтральная рампа

    val Neutral100 = Color(0xFFF3F5FE)
    val Neutral200 = Color(0xFFE4E7F5)
    val Neutral300 = Color(0xFFCFD3E5)
    val Neutral400 = Color(0xFFB2B6CA)
    val Neutral500 = Color(0xFF9397AB)
    val Neutral600 = Color(0xFF75798C)
    val Neutral700 = Color(0xFF595D6C)
    val Neutral800 = Color(0xFF3F424D)
    val Neutral900 = Color(0xFF292B31)

    // Акцентная рампа

    val Accent100 = Color(0xFFF5F4FF)
    val Accent200 = Color(0xFFE7E5FE)
    val Accent300 = Color(0xFFD2CEFD)
    val Accent400 = Color(0xFFB5ABFC)
    val Accent500 = Color(0xFF968AE0)
    val Accent600 = Color(0xFF796CBF)
    val Accent700 = Color(0xFF5D5294)
    val Accent800 = Color(0xFF423A6A)
    val Accent900 = Color(0xFF2B2741)

    // Поверхности глубже основной

    /** Тёплое пятно в левом верхнем углу экрана — единственный источник «света» на фоне. */
    val BackgroundGlow = Color(0xFF1E2134)

    /**
     * Подложка подписи под плиткой галереи: темнее фона, чтобы текст читался
     * поверх любой картинки.
     */
    val TileCaption = Color(0xFF0C0D14).copy(alpha = 0.74f)

    /** Дно прогресс-кольца и другие «утопленные» участки. */
    val SurfaceSunken = Color(0xFF1A1C2B)

    /**
     * Ячейка внутри карточки: на ступень темнее поверхности, но светлее фона
     * экрана — так вложенный блок читается как часть карточки, а не как дырка
     * в ней.
     */
    val SurfaceInset = Color(0xFF20222F)

    /**
     * Поверхность нижней шторки — на ступень светлее фона, чтобы её край
     * читался поверх затемнённого экрана.
     */
    val Sheet = Color(0xFF1C1E2C)

    /** Всплывающее меню альбомов: полупрозрачная подложка под размытием. */
    val Menu = Color(0xFF303242).copy(alpha = 0.72f)

    /** Панель нижнего меню — полупрозрачная, поверх неё работает размытие. */
    val Chrome = Color(0xFF141622).copy(alpha = 0.92f)

    /** Подложка под кнопкой действия: контент под ней должен «уходить в туман». */
    val Scrim = Color(0xFF161826).copy(alpha = 0.96f)

    // Знак приложения

    /**
     * Приглушённые квадраты знака. Холоднее нейтральной рампы: рядом
     * с акцентом чистый серый выглядит грязным.
     */
    val MarkMuted = Color(0xFF5B5F80)

    /**
     * Центр свечения на заставке. Ярче, чем [BackgroundGlow] у экранов:
     * на заставке нет контента, и вся глубина держится на этом пятне.
     */
    val SplashGlow = Color(0xFF24273D)

    /** Заливка галочки на акцентном кружке — тёмная, а не чёрная. */
    val OnAccent = Color(0xFF181A2C)

    // Вводный тур

    /**
     * Центр свечения на экране тура. Оно идёт сверху по центру, а не от угла:
     * на этих экранах смотрят в иллюстрацию, и свет собран вокруг неё.
     */
    val TourGlow = Color(0xFF23253A)

    /**
     * Низ экрана тура — темнее основного фона, чтобы кнопка внизу лежала
     * на самом глухом участке и читалась без подложки.
     */
    val TourEdge = Color(0xFF101220)

    /**
     * Лист бумаги в иллюстрациях: скан документа и карточка формата.
     * Единственное светлое пятно в тёмной системе — поэтому не чисто белое,
     * а с холодной тенью к нижнему углу.
     */
    val PaperLight = Color(0xFFDCDCE2)
    val PaperDark = Color(0xFFA9A9B4)

    // Полоски-заглушки вместо превью

    val StripeLight = Color(0xFF252838)
    val StripeDark = Color(0xFF1F2231)
    val StripeThumbLight = Color(0xFF292C3D)
    val StripeThumbDark = Color(0xFF222533)
}
