plugins {
    id("slimly.android.library")
}

dependencies {
    // `BackHandler` и `PredictiveBackHandler` живут здесь: системный жест
    // назад — событие Activity, а не Compose.
    implementation(libs.androidx.activity.compose)
}
