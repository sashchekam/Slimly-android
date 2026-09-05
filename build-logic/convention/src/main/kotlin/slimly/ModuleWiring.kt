package slimly

import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Подключает модуль к реестру: раздаёт объявленные зависимости и вешает
 * проверку графа на сборку.
 *
 * Отсюда следует главное отличие от обычного андроидного проекта: в блоке
 * `dependencies { }` самого модуля межмодульных связей нет — их источник
 * один, и он же проверяется.
 */
fun Project.wireModuleFromRegistry() {
    val module = SlimlyModule.of(path)
        ?: error(
            "Модуль $path не объявлен в реестре SlimlyModule " +
                "(build-logic/convention/src/main/kotlin/slimly/SlimlyModule.kt)."
        )

    module.dependencies.forEach { dependency ->
        dependencies.add("implementation", project(dependency.path))
    }

    val check = tasks.register<CheckModuleGraphTask>("checkModuleGraph") {
        group = "verification"
        description = "Сверяет зависимости модуля с реестром SlimlyModule."
        modulePath.set(module.path)
        declared.set(module.dependencies.map { it.path }.toSet())
    }

    // Фактические связи известны только после того, как отработал
    // `build.gradle.kts` модуля: именно туда их и дописывают руками.
    afterEvaluate {
        check.configure { actual.set(CheckModuleGraphTask.projectDependencies(this@wireModuleFromRegistry)) }
    }

    tasks.named("preBuild") { dependsOn(check) }
}

/** Пространство имён модуля по реестру. */
fun Project.slimlyNamespace(): String =
    SlimlyModule.of(path)?.namespace
        ?: error("Модуль $path не объявлен в реестре SlimlyModule.")
