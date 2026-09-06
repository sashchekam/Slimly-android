package com.slimly.core.deeplink

import android.net.Uri

/**
 * Разобранная ссылка вида `slimly://welcome?source=push`.
 *
 * Слой намеренно «глупый»: он ничего не знает про экраны и маршруты —
 * раскладка в навигацию живёт в `:app`, composition root.
 */
data class DeeplinkUrl(val host: String, val pathSegments: List<String>, val queryItems: Map<String, String>) {
    operator fun get(key: String): String? = queryItems[key]

    companion object {
        const val SCHEME = "slimly"

        /** `null` — ссылка не наша или в ней нет хоста, а значит и адреса. */
        fun of(uri: Uri): DeeplinkUrl? {
            val host = uri.host
            if (uri.scheme?.lowercase() != SCHEME || host.isNullOrEmpty()) return null

            return DeeplinkUrl(
                host = host,
                pathSegments = uri.pathSegments.orEmpty(),
                queryItems = uri.queryParameterNames.orEmpty()
                    .mapNotNull { name -> uri.getQueryParameter(name)?.let { name to it } }
                    .toMap(),
            )
        }
    }
}
