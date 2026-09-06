package slimly

/**
 * Единый реестр модулей приложения — андроидный аналог `Module.swift` из iOS.
 *
 * Состав сборки, зависимости и пространства имён берутся отсюда, а не
 * из отдельных `build.gradle.kts`: связи модулей видно одним списком,
 * и правило «фича не видит фичу» проверяется по этому же списку.
 *
 * Правила связей (архитектура B, см. ARCHITECTURE.md):
 * - core-модули не знают ничего о фичах;
 * - фича зависит от core-модулей и от `:api`-модулей чужих фич,
 *   но никогда — от их реализации;
 * - `:api` — тонкий модуль без UI: только маршруты и публичные контракты.
 */
enum class SlimlyModule(val path: String) {
    // Core
    Core(":core"),
    DesignSystem(":designsystem"),
    Navigation(":navigation"),
    Firebase(":firebase"),
    MediaKit(":mediakit"),
    MediaFlow(":mediaflow"),
    Shortcuts(":shortcuts"),
    Widget(":widget"),

    // Features: пара `:api` и `:impl` вместо иосных `Interface` и `Sources`.
    HomeApi(":feature:home:api"),
    Home(":feature:home:impl"),
    ProfileApi(":feature:profile:api"),
    Profile(":feature:profile:impl"),
    CompressApi(":feature:compress:api"),
    Compress(":feature:compress:impl"),
    ConvertApi(":feature:convert:api"),
    Convert(":feature:convert:impl"),
    OnboardingApi(":feature:onboarding:api"),
    Onboarding(":feature:onboarding:impl"),
    AudioApi(":feature:audio:api"),
    Audio(":feature:audio:impl"),
    ScanApi(":feature:scan:api"),
    Scan(":feature:scan:impl"),
    DebugApi(":feature:debug:api"),
    Debug(":feature:debug:impl"),

    // Composition root
    App(":app"),
    ;

    enum class Kind { CORE, FEATURE_API, FEATURE, APP }

    val kind: Kind
        get() = when (this) {
            Core, DesignSystem, Navigation, Firebase, MediaKit, MediaFlow, Shortcuts, Widget -> Kind.CORE
            HomeApi, ProfileApi, CompressApi, ConvertApi, OnboardingApi, AudioApi, ScanApi, DebugApi ->
                Kind.FEATURE_API

            Home, Profile, Compress, Convert, Onboarding, Audio, Scan, Debug -> Kind.FEATURE
            App -> Kind.APP
        }

    /**
     * Пространство имён модуля. Собирается из пути, но без хвостового `impl`:
     * реализация и её интерфейс — это одна фича, и в пакете она одна.
     */
    val namespace: String
        get() = path.removePrefix(":")
            .split(":")
            .filterNot { it == "impl" }
            .joinToString(separator = ".", prefix = "com.slimly.")

    val dependencies: List<SlimlyModule>
        get() = when (this) {
            Core -> emptyList()
            DesignSystem -> listOf(Core)
            // Роутер рисует себя сам — заглушка неразрешённого маршрута
            // и шторка берут цвета оттуда же, откуда экраны.
            Navigation -> listOf(Core, DesignSystem)
            // Firebase виден приложению только через протоколы из core:
            // ни один SDK за границу модуля не выходит.
            Firebase -> listOf(Core)
            // Единственный модуль, знающий MediaStore и кодировщики. Наружу
            // отдаёт только реализации контрактов из core — как и Firebase.
            MediaKit -> listOf(Core)
            // Общие шаги обработки. MediaKit сюда не входит намеренно:
            // шаги работают с контрактом медиатеки, а не с MediaStore.
            MediaFlow -> listOf(Core, DesignSystem, Navigation)
            // Каталог инструментов — чистые данные: подписи, значки и адреса.
            // Ни одного экрана, поэтому и дизайн-системы ему не нужно.
            Shortcuts -> listOf(Core)
            // Виджет рисуется не Compose, а Glance, и живёт в чужом процессе.
            // Фичи ему не видны вовсе: инструмент он открывает ссылкой.
            Widget -> listOf(Core, DesignSystem, Shortcuts)
            // Интерфейс фичи знает модели из core и тип маршрута из навигации:
            // маршрут несёт данные, но не умеет рисовать и не тянет за собой
            // дизайн-систему.
            HomeApi -> listOf(Core, Navigation)
            // Главная умеет открыть профиль — но видит от него только маршрут.
            Home -> listOf(Core, DesignSystem, Navigation, HomeApi, ProfileApi)
            ProfileApi -> listOf(Core, Navigation)
            Profile -> listOf(Core, DesignSystem, Navigation, ProfileApi)
            CompressApi -> listOf(Core, Navigation)
            // Своего у сжатия — одна шторка со степенью: остальные шаги общие
            // и приходят из :mediaflow.
            Compress -> listOf(Core, DesignSystem, Navigation, MediaFlow, CompressApi)
            ConvertApi -> listOf(Core, Navigation)
            // Конвертации своих экранов не досталось вовсе: и выбор формата
            // общий — у неё только состав шагов.
            Convert -> listOf(Core, Navigation, MediaFlow, ConvertApi)
            OnboardingApi -> listOf(Core, Navigation)
            // Тур заканчивается сменой корня на главный экран, но какая фича
            // за ним стоит, ему знать незачем: маршрут приходит параметром.
            Onboarding -> listOf(Core, DesignSystem, Navigation, OnboardingApi)
            AudioApi -> listOf(Core, Navigation)
            // Как и у конвертации, своих экранов нет: все четыре шага общие,
            // а фича задаёт только их состав.
            Audio -> listOf(Core, Navigation, MediaFlow, AudioApi)
            ScanApi -> listOf(Core, Navigation)
            // Единственный инструмент со своими экранами поверх общих шагов:
            // съёмка страниц и правка границ есть только у него. Камеру
            // и поиск листа он получает контрактами из core — про `:mediakit`
            // фиче знать незачем, как и всем остальным.
            Scan -> listOf(Core, DesignSystem, Navigation, MediaFlow, ScanApi)
            DebugApi -> listOf(Core, Navigation)
            // Сервисный экран: подмены, значения конфига и журнал аналитики
            // — всё это уже лежит в `:core`, и своего у меню только показ.
            Debug -> listOf(Core, DesignSystem, Navigation, DebugApi)
            // Composition root видит всех: он собирает граф зависимостей
            // и раздаёт фичам их резолверы.
            App -> listOf(
                Core,
                DesignSystem,
                Navigation,
                Firebase,
                MediaKit,
                MediaFlow,
                Shortcuts,
                Widget,
                HomeApi,
                Home,
                ProfileApi,
                Profile,
                CompressApi,
                Compress,
                ConvertApi,
                Convert,
                OnboardingApi,
                Onboarding,
                AudioApi,
                Audio,
                ScanApi,
                Scan,
                DebugApi,
                Debug,
            )
        }

    companion object {
        fun of(path: String): SlimlyModule? = entries.firstOrNull { it.path == path }

        /**
         * Все модули, проверенные на соблюдение правил связей.
         * Нарушение валит конфигурацию сборки с внятным сообщением — так же,
         * как на iOS его валит `tuist generate`.
         */
        fun validated(): List<SlimlyModule> {
            entries.forEach { module ->
                module.dependencies.forEach { dependency ->
                    require(isAllowed(module, dependency)) {
                        forbidden(module.path, dependency.path)
                    }
                }
            }
            return entries
        }

        fun isAllowed(module: SlimlyModule, dependency: SlimlyModule): Boolean =
            when (module.kind) {
                Kind.CORE -> dependency.kind == Kind.CORE
                Kind.FEATURE_API -> dependency.kind == Kind.CORE
                Kind.FEATURE -> dependency.kind == Kind.CORE || dependency.kind == Kind.FEATURE_API
                Kind.APP -> true
            }

        fun forbidden(module: String, dependency: String): String =
            """
            |Запрещённая зависимость: $module → $dependency.
            |Core не может знать о фичах, а фича — о реализации чужой фичи
            |(только о её :api-модуле).
            """.trimMargin()
    }
}
