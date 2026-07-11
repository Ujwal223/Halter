package com.ujwal.halter.service

import java.net.URI

object DomainExtractor {

    fun extract(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim()

        s = s.trimStart { !it.isLetterOrDigit() }

        val placeholderMarkers = listOf(
            "search", "type", "type a url", "search or type",
            "search the web"
        )
        val lower = s.lowercase()
        if (placeholderMarkers.any { it.isNotBlank() && lower == it }) return null
        if (!s.contains(".") ) return null

        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://$s"
        }

        return try {
            val host = URI(s).host ?: return null
            host.removePrefix("www.").lowercase()
        } catch (e: Exception) {
            null
        }
    }
}
