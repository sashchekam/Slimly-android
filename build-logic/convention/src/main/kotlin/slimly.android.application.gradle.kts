import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import slimly.SlimlySdk
import slimly.slimlyNamespace
import slimly.wireModuleFromRegistry

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("slimly.quality")
    id("slimly.analytics")
}

/**
 * Firebase включается наличием файла ключей, а не флагом.
 *
 * `google-services` без `google-services.json` валит сборку с требованием
 * этого файла, а клон на чужой машине должен собираться и запускаться —
 * просто без аналитики. Появился файл — появились аналитика, удалённый
 * конфиг и отчёты о падениях; код приложения об этом не знает вовсе:
 * `FirebaseApp.initializeApp` сам разберётся, есть ли ключи.
 */
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = slimlyNamespace()
    compileSdk = SlimlySdk.COMPILE

    defaultConfig {
        applicationId = "com.slimly"
        minSdk = SlimlySdk.MIN
        targetSdk = SlimlySdk.TARGET
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // `BuildConfig.DEBUG` решает состав сервисов: в отладочной сборке
        // аналитика дублируется в лог, а конфиг пускает поверх себя подмены.
        buildConfig = true
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
    add("implementation", platform(libs.findLibrary("compose-bom").get()))
    add("implementation", libs.findLibrary("compose-ui").get())
    add("implementation", libs.findLibrary("compose-foundation").get())
    add("implementation", libs.findLibrary("compose-material3").get())
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    add("implementation", libs.findLibrary("androidx-core-ktx").get())
    add("implementation", libs.findLibrary("androidx-activity-compose").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
    add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
    add("lintChecks", libs.findLibrary("compose-lint-checks").get())

    add("testImplementation", libs.findLibrary("junit").get())
}

wireModuleFromRegistry()
