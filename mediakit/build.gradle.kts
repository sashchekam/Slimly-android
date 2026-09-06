plugins {
    id("slimly.android.library")
}

dependencies {
    // `ContextCompat.checkSelfPermission`: проверка разрешения без ветки
    // по версии системы.
    implementation(libs.androidx.core.ktx)

    // Метаданные снимка поверх готового файла: `Bitmap` их не носит.
    implementation(libs.androidx.exifinterface)

    // Перекодирование видео. `effect` нужен ради `Presentation` — им задаётся
    // размер кадра результата.
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.common)

    // Камера скана: превью, снимок и поток кадров для поиска листа.
    // `view` приносит контроллер, который держит эти три дела вместе.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}
