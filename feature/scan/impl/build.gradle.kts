plugins {
    id("slimly.android.library")
}

dependencies {
    // Разрешение на камеру и переход в системные настройки после отказа
    // показывает Activity — экран съёмки запускает их своим лаунчером.
    // Тем же путём приезжает и `BackHandler`: уход со съёмки не должен
    // ронять стек скана целиком.
    implementation(libs.androidx.activity.compose)
}
