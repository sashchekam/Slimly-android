import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import slimly.SlimlySdk
import slimly.slimlyNamespace
import slimly.wireModuleFromRegistry

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("slimly.quality")
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
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    add("implementation", libs.findLibrary("androidx-core-ktx").get())
    add("implementation", libs.findLibrary("androidx-activity-compose").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
    add("lintChecks", libs.findLibrary("compose-lint-checks").get())
}

wireModuleFromRegistry()
