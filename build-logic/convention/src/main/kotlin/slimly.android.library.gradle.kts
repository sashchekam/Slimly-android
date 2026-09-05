import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import slimly.SlimlySdk
import slimly.slimlyNamespace
import slimly.wireModuleFromRegistry

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
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
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    add("lintChecks", libs.findLibrary("compose-lint-checks").get())
}

wireModuleFromRegistry()
