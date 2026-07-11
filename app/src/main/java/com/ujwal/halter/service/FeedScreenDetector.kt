package com.ujwal.halter.service

import android.view.accessibility.AccessibilityNodeInfo

object FeedScreenDetector {

    fun isOnFeedScreen(root: AccessibilityNodeInfo, packageName: String, userAddedPackages: Set<String>): Boolean {
        val signature = FeedScreenSignatures.forPackage(packageName)
        if (signature != null) {
            return matchesSignature(root, signature)
        }
        if (packageName in userAddedPackages) {
            return genericFullscreenFeedHeuristic(root)
        }
        return false
    }

    private fun matchesSignature(root: AccessibilityNodeInfo, sig: FeedSignature): Boolean {
        for (exclId in sig.exclusionIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(exclId)
            val present = !nodes.isNullOrEmpty()
            nodes?.forEach { it.recycle() }
            if (present) return false
        }
        for (id in sig.feedIndicatorIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            val present = !nodes.isNullOrEmpty()
            nodes?.forEach { it.recycle() }
            if (present) return true
        }
        return false
    }

    private fun genericFullscreenFeedHeuristic(root: AccessibilityNodeInfo): Boolean {
        val screenBounds = android.graphics.Rect().also { root.getBoundsInScreen(it) }
        var found = false

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (found || depth > 30) return
            val cls = node.className?.toString().orEmpty()
            val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }

            val isFullscreenContainer =
                (cls.contains("RecyclerView") || cls.contains("ViewPager") || cls.contains("Player")) &&
                    bounds.height() >= (screenBounds.height() * 0.85) &&
                    bounds.width() >= (screenBounds.width() * 0.85)

            if (isFullscreenContainer) {
                found = true
                return
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
                child.recycle()
            }
        }
        visit(root, 0)
        return found
    }
}
