package com.ujwal.halter.service

import android.view.accessibility.AccessibilityNodeInfo

object UrlBarLocator {

    private val KNOWN_URL_BAR_IDS: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
        "com.chrome.beta" to listOf("com.android.chrome:id/url_bar"),
        "com.chrome.dev" to listOf("com.android.chrome:id/url_bar"),
        "com.brave.browser" to listOf("com.brave.browser:id/url_bar"),
        "com.microsoft.emmx" to listOf("com.microsoft.emmx:id/url_bar"),
        "com.opera.browser" to listOf("com.opera.browser:id/url_field"),
        "com.opera.mini.native" to listOf("com.opera.mini.native:id/url_field"),
        "com.vivaldi.browser" to listOf("com.vivaldi.browser:id/url_bar"),
        "com.kiwibrowser.browser" to listOf("com.android.chrome:id/url_bar"),
        "org.mozilla.firefox" to listOf("org.mozilla.firefox:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.firefox_beta" to listOf("org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.fenix" to listOf("org.mozilla.fenix:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.focus" to listOf("org.mozilla.focus:id/display_url", "org.mozilla.focus:id/url_edit"),
        "us.spotco.fennec_dos" to listOf("us.spotco.fennec_dos:id/mozac_browser_toolbar_url_view"),
        "com.sec.android.app.sbrowser" to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        "com.duckduckgo.mobile.android" to listOf("com.duckduckgo.mobile.android:id/omnibarTextInput"),
        "com.UCMobile.intl" to listOf("com.UCMobile.intl:id/address_bar_edit_text"),
        "app.vanadium.browser" to listOf("org.chromium.chrome:id/url_bar", "app.vanadium.browser:id/url_bar"),
        "org.chromium.chrome" to listOf("org.chromium.chrome:id/url_bar")
    )

    fun findUrlText(root: AccessibilityNodeInfo, packageName: String): String? {
        KNOWN_URL_BAR_IDS[packageName]?.forEach { id ->
            val text = tryFindById(root, id)
            if (text != null) return text
        }

        return heuristicScan(root)
    }

    private fun tryFindById(root: AccessibilityNodeInfo, id: String): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(id) ?: return null
        try {
            return nodes.firstOrNull { it.text != null }?.text?.toString()
        } finally {
            nodes.forEach { it.recycle() }
        }
    }

    private fun heuristicScan(root: AccessibilityNodeInfo): String? {
        val screenHeight = run {
            val b = android.graphics.Rect()
            root.getBoundsInScreen(b)
            if (b.height() > 0) b.height() else 1920
        }
        val topBandLimit = (screenHeight * 0.15).toInt()

        var best: String? = null
        var bestScore = 0

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 40) return
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)

            val className = node.className?.toString().orEmpty()
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()?.lowercase().orEmpty()

            val looksLikeUrlField =
                className.contains("EditText") ||
                className.contains("TextView") && (
                    desc.contains("address") || desc.contains("url") ||
                    desc.contains("search or type")
                )

            if (looksLikeUrlField && bounds.top in 0..topBandLimit) {
                val candidate = text ?: node.contentDescription?.toString()
                val domainLike = DomainExtractor.extract(candidate)
                if (domainLike != null) {
                    var score = 1
                    if (className.contains("EditText")) score += 2
                    if (desc.contains("address") || desc.contains("url")) score += 2
                    if (score > bestScore) {
                        bestScore = score
                        best = candidate
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
                child.recycle()
            }
        }

        visit(root, 0)
        return best
    }
}
