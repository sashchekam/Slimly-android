import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension

// Замена SwiftLint: detekt следит за формой кода, ktlint — за форматированием,
// compose-lints (внутри андроидного Lint) — за формой Composable-функций.
// Все трое в строгом режиме и висят на сборке: замечание валит билд.
//
// detekt взят второй мажорной версии (пока альфа) не из любви к свежему:
// 1.23 не запускается на JDK 25, который приносит с собой Android Studio.
plugins {
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    parallel.set(true)
    ignoreFailures.set(false)
}

tasks.withType<Detekt>().configureEach {
    jvmTarget.set("17")
    reports {
        html.required.set(false)
        sarif.required.set(false)
    }
}

ktlint {
    version.set(libs.findVersion("ktlint").get().requiredVersion)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

// Линтеры висят на сборке, а не только на `check`: как SwiftLint фазой
// сборки на iOS. Замечание видно там же, где ошибка компилятора.
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(tasks.named("detekt"))
    dependsOn(tasks.named("ktlintCheck"))
}
