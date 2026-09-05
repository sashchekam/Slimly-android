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
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
}
