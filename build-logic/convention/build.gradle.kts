plugins {
    `kotlin-dsl`
}

group = "com.slimly.buildlogic"

dependencies {
    // Плагины приходят сюда целиком, а не `compileOnly`: конвенционный
    // плагин настраивает их своими типами (`detekt { }`, `android { }`),
    // и эти классы нужны ему в рантайме. Основная сборка их не объявляет —
    // единственный источник версий отсюда, двух копий AGP не бывает.
    implementation(libs.android.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    // Плагин сериализации, в отличие от `parcelize`, вместе с AGP не приезжает:
    // его реализация лежит в отдельном артефакте.
    implementation(libs.kotlin.serialization.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    // Плагины Firebase лежат на класспути всегда, а применяются только
    // при наличии `google-services.json`: решает это `slimly.android.application`.
    implementation(libs.google.services.gradlePlugin)
    implementation(libs.firebase.crashlytics.gradlePlugin)
}
