import org.gradle.kotlin.dsl.register
import slimly.AnalyticsRegistryTask

/**
 * Реестр событий аналитики. Живёт задачей сборки, а не отдельным скриптом,
 * как на iOS: у Gradle уже есть и обход исходников, и место, куда повесить
 * проверку.
 *
 * Подключается к `:app`: реестр один на приложение, а исходники задача берёт
 * из всех модулей сразу.
 */
val analyticsSources = rootProject.fileTree(rootProject.projectDir) {
    include("**/src/main/kotlin/**/*Analytics.kt")
    exclude("**/build/**")
}

val analyticsRegistry = rootProject.layout.projectDirectory.file("ANALYTICS.md")

/** Реестр соседней платформы — если репозитории лежат рядом. */
val iosAnalyticsRegistry = rootProject.layout.projectDirectory.file("../ios/ANALYTICS.md")

tasks.register<AnalyticsRegistryTask>("generateAnalyticsRegistry") {
    group = "documentation"
    description = "Собирает ANALYTICS.md из аналитики модулей."
    sources.from(analyticsSources)
    projectRoot.set(rootProject.layout.projectDirectory)
    registry.set(analyticsRegistry)
    checkOnly.set(false)
    if (iosAnalyticsRegistry.asFile.exists()) iosRegistry.set(iosAnalyticsRegistry)
}

val checkAnalyticsRegistry = tasks.register<AnalyticsRegistryTask>("checkAnalyticsRegistry") {
    group = "verification"
    description = "Проверяет, что ANALYTICS.md собран из текущего кода."
    sources.from(analyticsSources)
    projectRoot.set(rootProject.layout.projectDirectory)
    registry.set(analyticsRegistry)
    checkOnly.set(true)
    if (iosAnalyticsRegistry.asFile.exists()) iosRegistry.set(iosAnalyticsRegistry)
}

tasks.named("preBuild") { dependsOn(checkAnalyticsRegistry) }
