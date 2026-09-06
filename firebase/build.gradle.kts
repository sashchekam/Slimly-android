plugins {
    id("slimly.android.library")
}

dependencies {
    // Единственный модуль, который видит SDK. Реестр раздаёт связи между
    // модулями, а внешние библиотеки объявляет тот, кому они нужны, — иначе
    // Firebase приехал бы в каждый модуль сборки.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    // Отчёты о падениях. Своего кода у них нет: SDK поднимается
    // сам вместе с приложением и молчит, пока падений нет.
    implementation(libs.firebase.crashlytics)
    // `Task.await()`: SDK Firebase говорит задачами Google Play, а не
    // корутинами. Свой мостик писать незачем — он уже есть.
    implementation(libs.kotlinx.coroutines.play.services)
}
