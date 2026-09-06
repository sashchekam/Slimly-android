import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import slimly.SlimlySdk
import slimly.slimlyNamespace
import slimly.wireModuleFromRegistry

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    // Маршрут переживает смерть процесса, поэтому он `Parcelable`, а писать
    // `writeToParcel` руками для каждого маршрута незачем.
    id("org.jetbrains.kotlin.plugin.parcelize")
    // Значения без примитивного вида уезжают в хранилище как JSON — замена
    // иосного `Codable`.
    id("org.jetbrains.kotlin.plugin.serialization")
    id("slimly.quality")
}

android {
    namespace = slimlyNamespace()
    compileSdk = SlimlySdk.COMPILE

    defaultConfig {
        minSdk = SlimlySdk.MIN
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }

    testOptions {
        unitTests {
            // Тесты роутера трогают `Parcelable`-маршруты, но не сам `Parcel`.
            // Без этого любой заглушённый метод android.jar валит тест.
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // Compose есть у каждого модуля, включая `:core`: состояние вьюмоделей
    // и роутера живёт в `mutableStateOf` — это рантайм Compose, а не UI.
    add("implementation", platform(libs.findLibrary("compose-bom").get()))
    add("implementation", libs.findLibrary("compose-ui").get())
    add("implementation", libs.findLibrary("compose-foundation").get())
    add("implementation", libs.findLibrary("compose-material3").get())
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    add("lintChecks", libs.findLibrary("compose-lint-checks").get())

    add("testImplementation", libs.findLibrary("junit").get())
    add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
}

wireModuleFromRegistry()
