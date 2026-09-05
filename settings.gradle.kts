pluginManagement {
    // Конвенционные плагины живут отдельной сборкой: они компилируются
    // до основного проекта и потому могут собой этот проект настраивать.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Репозитории объявляются один раз на всю сборку: модуль, который заводит
    // свой, роняет конфигурацию — иначе источник зависимости у каждого свой.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

// Состав сборки не перечисляется руками: модули берутся из реестра
// (`build-logic/.../SlimlyModule.kt`) — андроидный аналог `Module.swift`.
plugins {
    id("slimly.modules")
}

rootProject.name = "Slimly"
