package slimly

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Сверяет фактические зависимости модуля с реестром.
 *
 * Реестр раздаёт зависимости сам, поэтому любая связь, дописанная руками
 * в `build.gradle.kts`, здесь оказывается лишней — и сборка падает. Так
 * правило «фича не видит фичу» держится с обеих сторон: запрещённое ребро
 * в реестре не пройдёт проверку правил, а мимо реестра его не провести.
 */
abstract class CheckModuleGraphTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    /** Что модулю положено по реестру. */
    @get:Input
    abstract val declared: SetProperty<String>

    /** Что у модуля есть на самом деле — все `project(...)` из его конфигураций. */
    @get:Input
    abstract val actual: SetProperty<String>

    @TaskAction
    fun check() {
        val path = modulePath.get()
        val module = SlimlyModule.of(path)
            ?: throw GradleException("Модуль $path не объявлен в реестре SlimlyModule.")

        val extra = actual.get() - declared.get()
        if (extra.isEmpty()) return

        val problems = extra.sorted().map { dependencyPath ->
            val dependency = SlimlyModule.of(dependencyPath)
            when {
                dependency == null ->
                    "Зависимость $path → $dependencyPath ведёт в модуль вне реестра."

                !SlimlyModule.isAllowed(module, dependency) ->
                    SlimlyModule.forbidden(path, dependencyPath)

                else ->
                    "Зависимость $path → $dependencyPath дописана руками. " +
                        "Связи модулей объявляются в реестре SlimlyModule, а не в build.gradle.kts."
            }
        }

        throw GradleException(problems.joinToString(separator = "\n\n"))
    }

    companion object {
        /**
         * Все `project(...)`-зависимости проекта, из всех конфигураций.
         *
         * Ссылка на себя не считается: её AGP заводит сам — тестовый вариант
         * модуля зависит от самого модуля.
         */
        fun projectDependencies(project: org.gradle.api.Project): Set<String> =
            project.configurations
                .flatMap { configuration ->
                    configuration.dependencies.withType(ProjectDependency::class.java).map { it.path }
                }
                .filterNot { it == project.path }
                .toSet()
    }
}
