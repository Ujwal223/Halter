package com.ujwal.halter.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Determines whether a given package is a browser WITHOUT a hardcoded list.
 */
object BrowserRegistry {

    private var cache: MutableSet<String>? = null

    fun isBrowser(context: Context, packageName: String): Boolean {
        val set = cache ?: buildBrowserSet(context).also { cache = it }
        return packageName in set
    }

    /** Call this from a BroadcastReceiver on PACKAGE_ADDED/REMOVED/REPLACED */
    fun invalidateCache() {
        cache = null
    }

    private fun buildBrowserSet(context: Context): MutableSet<String> {
        val pm = context.packageManager
        val result = mutableSetOf<String>()

        for (scheme in listOf("http", "https")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com"))
            val resolves = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            resolves.forEach { info ->
                info.activityInfo?.packageName?.let { result.add(it) }
            }
        }

        val excluded = setOf(
            "com.google.android.apps.docs",
            "com.google.android.apps.pdfviewer",
            "com.android.chrome.dev"
        )
        result.removeAll(excluded)

        return result
    }
}
