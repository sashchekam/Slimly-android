plugins {
    id("slimly.android.library")
}

dependencies {
    // Glance нужен только здесь: он рисует не Compose-деревом, а RemoteViews,
    // и остальным модулям бесполезен.
    implementation(libs.androidx.glance.appwidget)

    // Экран настройки виджета — обычная Activity на Compose: система
    // показывает его при добавлении ярлыка, и Glance для него не годится.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
