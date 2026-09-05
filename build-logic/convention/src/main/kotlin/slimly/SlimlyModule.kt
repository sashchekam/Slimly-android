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

    // Features: пара `:api` и `:impl` вместо иосных `Interface` и `Sources`.
    HomeApi(":feature:home:api"),
    Home(":feature:home:impl"),
    ProfileApi(":feature:profile:api"),
    Profile(":feature:profile:impl"),

    // Composition root
    App(":app"),
    ;

    enum class Kind { CORE, FEATURE_API, FEATURE, APP }

    val kind: Kind
        get() = when (this) {
            Core, DesignSystem -> Kind.CORE
            HomeApi, ProfileApi -> Kind.FEATURE_API
            Home, Profile -> Kind.FEATURE
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
            // Интерфейс фичи знает только модели из core: маршрут несёт данные,
            // но не умеет рисовать и не тянет за собой дизайн-систему.
            HomeApi -> listOf(Core)
            Home -> listOf(Core, DesignSystem, HomeApi)
            ProfileApi -> listOf(Core)
            Profile -> listOf(Core, DesignSystem, ProfileApi)
            // Composition root видит всех: он собирает граф зависимостей
            // и раздаёт фичам их резолверы.
            App -> listOf(Core, DesignSystem, HomeApi, Home, ProfileApi, Profile)
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
