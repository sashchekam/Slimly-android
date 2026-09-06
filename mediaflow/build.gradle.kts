plugins {
    id("slimly.android.library")
}

dependencies {
    // Системные запросы — разрешения и удаление оригиналов — показывает
    // Activity: экран запускает их своим лаунчером.
    implementation(libs.androidx.activity.compose)
}
