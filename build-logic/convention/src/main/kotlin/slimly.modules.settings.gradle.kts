import slimly.SlimlyModule

// Состав сборки — это реестр, и ничего кроме него: папка без записи
// в реестре в сборку не попадёт, запись без папки уронит конфигурацию.
SlimlyModule.validated().forEach { module ->
    include(module.path)
}
