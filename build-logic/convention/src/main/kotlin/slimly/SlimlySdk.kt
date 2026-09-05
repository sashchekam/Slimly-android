package slimly

/**
 * Версии платформы — одни на все модули.
 *
 * `minSdk = 29`: начиная с Android 10 у медиатеки единственная модель
 * хранилища (scoped storage), и веток под старую заводить не придётся.
 */
object SlimlySdk {
    const val COMPILE = 37
    const val TARGET = 37
    const val MIN = 29
}
