package com.slimly.feature.scan

import com.slimly.feature.scan.api.ScanRoute
import com.slimly.navigation.Router

class ScanIntroViewModel(private val analytics: ScanAnalytics, private val router: Router) {
    /**
     * Слой, который показал шторку: сам источник открывается уже в нём.
     * Внутри шторки ни камере, ни галерее места нет — они не продолжение
     * выбора, а замена ему.
     */
    private val presenter: Router
        get() = router.parent ?: router

    /**
     * Камера показывается поверх флоу: у неё нет ни шапки, ни полей экрана,
     * и в стеке она выглядела бы провалом в другое приложение.
     */
    fun openCamera() {
        analytics.didPickSource(ScanSource.CAMERA)

        val presenter = presenter
        router.dismiss()
        presenter.cover(ScanRoute.Camera)
    }

    fun openGallery() {
        analytics.didPickSource(ScanSource.GALLERY)

        val presenter = presenter
        router.dismiss()
        presenter.push(ScanRoute.Picker)
    }

    fun cancel() {
        router.dismiss()
    }
}
