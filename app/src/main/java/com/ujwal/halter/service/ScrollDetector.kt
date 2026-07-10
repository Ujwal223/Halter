// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.view.accessibility.AccessibilityNodeInfo
import com.ujwal.halter.data.ContentType
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/** Known app package names that have built-in scroll/short-video detection. */
object KnownScrollApps {
    val all = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "app.revanced.android.youtube",
        "app.rvx.android.youtube",
        "com.facebook.katana",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.snapchat.android",
        "com.pinterest",
        "com.twitter.android"
    )
    val labels = mapOf(
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube",
        "app.revanced.android.youtube" to "YouTube ReVanced",
        "app.rvx.android.youtube" to "YouTube ReVanced Extended",
        "com.facebook.katana" to "Facebook",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok (Lite)",
        "com.snapchat.android" to "Snapchat",
        "com.pinterest" to "Pinterest",
        "com.twitter.android" to "X / Twitter"
    )
}

class ScrollDetector(
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = SystemClock
) {
    private val debouncer = ScrollDebouncer()

    suspend fun shouldCount(packageName: String, contentType: ContentType): Boolean {
        val debounce = settingsRepository.settings.first().scrollDebounceMillis
        return debouncer.shouldCount("$packageName:${contentType.name}", clock.now(), debounce)
    }

    private val customScrollPackages = mutableSetOf<String>()

    /** Update the set of custom packages the user added manually for scroll detection. */
    fun setCustomPackages(packages: Set<String>) {
        customScrollPackages.clear()
        customScrollPackages.addAll(packages)
    }

    /** View IDs that indicate short-form content (from Shorts-Blocker reference + our own detection). */
    private val youtubeShortsViewIds = listOf(
        // reel_progress_bar: present ONLY on the Shorts player screen — highest-confidence signal
        // (from Shorts-Blocker open-source reference, YouTubeShortsDetector.kt)
        "com.google.android.youtube:id/reel_progress_bar",
        "com.google.android.youtube:id/reel_recycler",
        "com.google.android.youtube:id/reel_player_page",
        "com.google.android.youtube:id/shorts_player",
        "com.google.android.youtube:id/shorts_container",
        "com.google.android.youtube:id/reel_watch_fragment",
        "com.google.android.youtube:id/reel_player",
        "app.revanced.android.youtube:id/reel_progress_bar",
        "app.revanced.android.youtube:id/reel_recycler",
        "app.revanced.android.youtube:id/reel_player_page",
        "app.rvx.android.youtube:id/reel_progress_bar",
        "app.rvx.android.youtube:id/reel_recycler"
    )

    private val instagramReelsViewIds = listOf(
        "com.instagram.android:id/clips_viewer_view_pager",
        "com.instagram.android:id/clips_viewer_viewer_container",
        "com.instagram.android:id/reel_viewer_root"
    )

    fun hasShortVideoSurface(packageName: String, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val viewIds = when (packageName) {
            "com.google.android.youtube", "app.revanced.android.youtube", "app.rvx.android.youtube" -> youtubeShortsViewIds
            "com.instagram.android" -> instagramReelsViewIds
            else -> emptyList()
        }
        for (id in viewIds) {
            val list = root.findAccessibilityNodeInfosByViewId(id)
            if (list.isNotEmpty()) {
                list.forEach { it.recycle() }
                return true
            }
        }
        return detectContentType(packageName, root).isShortVideo()
    }

    fun detectContentType(packageName: String, root: AccessibilityNodeInfo?): ContentType {
        if (root == null) return ContentType.UNKNOWN

        // Fast path: known view IDs (reliable for YouTube Shorts / IG Reels)
        when (packageName) {
            "com.google.android.youtube", "app.revanced.android.youtube", "app.rvx.android.youtube" -> {
                for (id in youtubeShortsViewIds) {
                    val list = root.findAccessibilityNodeInfosByViewId(id)
                    if (list.isNotEmpty()) {
                        list.forEach { it.recycle() }
                        return ContentType.SHORT
                    }
                }
            }
            "com.instagram.android" -> {
                for (id in instagramReelsViewIds) {
                    val list = root.findAccessibilityNodeInfosByViewId(id)
                    if (list.isNotEmpty()) {
                        list.forEach { it.recycle() }
                        return ContentType.REEL
                    }
                }
            }
        }

        val ids = mutableListOf<String>()
        val classNames = mutableListOf<String>()
        val texts = mutableListOf<String>()
        collectViewInfo(root, ids, classNames, texts)
        val joined = ids.joinToString(" ").lowercase()
        val classes = classNames.joinToString(" ").lowercase()
        val allText = texts.joinToString(" ").lowercase()

        // Known app detection
        val known = when (packageName) {
            "com.instagram.android" -> when {
                joined.contains("clips_viewer") || joined.contains("reel_viewer_root") -> ContentType.REEL
                joined.contains("feed") || joined.contains("tab") -> ContentType.FEED
                else -> ContentType.UNKNOWN
            }
            "com.google.android.youtube", "app.revanced.android.youtube", "app.rvx.android.youtube" -> when {
                joined.contains("shorts") || joined.contains("reel") || classes.contains("shorts") ||
                    allText.contains("shorts") || joined.contains("reel_recycler") -> ContentType.SHORT
                else -> ContentType.UNKNOWN
            }
            "com.facebook.katana" -> when {
                joined.contains("reels") || joined.contains("reel") || classes.contains("reel") -> ContentType.REEL
                joined.contains("feed") -> ContentType.FEED
                else -> ContentType.UNKNOWN
            }
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> ContentType.SHORT
            "com.snapchat.android" -> ContentType.SHORT
            "com.pinterest" -> ContentType.REEL
            "com.twitter.android" -> when {
                joined.contains("video") || joined.contains("player") || classes.contains("video") -> ContentType.SHORT
                else -> ContentType.UNKNOWN
            }
            else -> if (packageName in customScrollPackages) {
                // User explicitly added this app for scroll monitoring.
                // Return SHORT immediately — do not gate on heuristic text detection, because
                // custom apps won't have 'shorts'/'reels' in their UI text.
                ContentType.SHORT
            } else ContentType.UNKNOWN
        }

        // Log for debugging
        if (known == ContentType.FEED || known == ContentType.SHORT || known == ContentType.REEL) {
            android.util.Log.d("ScrollDetector", "Detected $known in $packageName (viewIds: $joined)")
        }
        return known
    }

    /**
     * Returns true if a non-feed overlay panel is visible in the accessibility tree.
     *
     * Covers: comment sheets, reply threads, about/description panels, share panels,
     * like lists, profile drawers, context menus, and search bars opened over the feed.
     *
     * Detection happens via THREE independent signals (any one is sufficient):
     *  1. View resource-ID contains a known panel keyword.
     *  2. Node text *contains* a comment/panel indicator phrase (covers localised strings).
     *  3. Content-description *contains* a panel indicator phrase (accessibility label path).
     */
    fun isSubViewActive(root: AccessibilityNodeInfo): Boolean {
        // View-ID substrings that strongly indicate a non-feed panel is open.
        val idKeywords = listOf(
            "comment_sheet", "comments_sheet", "comment_list", "comment_container", "comment_recycler", "comment_input", "comment_composer",
            "reply_composer", "reply_sheet", "reply_list", "reply_container",
            "profile_page", "user_profile", "channel_profile", "channel_info",
            "bottom_sheet", "sliding_panel", "panel_container", "side_panel",
            "dialog", "modal", "popup",
            "about_this", "about_video", "expanded_description", "description_panel",
            "share_sheet", "share_panel", "send_to", "direct_share",
            "like_list", "likers", "emoji_tray",
            "search_bar", "search_panel", "search_overlay",
            "context_menu", "overflow_menu", "menu_bottom_sheet",
            "engagement_panel",        // YouTube info panels (chapters, description, etc.)
            "thumbnail_overlays_container" // avoid counting YT thumbnail row changes
        )

        // Text substrings that indicate a non-feed overlay panel is open.
        // IMPORTANT: Only use phrases that are very specific to panel chrome, NOT general video
        // UI text. 'share', 'description', 'search' appear in video captions and cause false
        // suppression of valid scroll signals — so they are intentionally excluded here.
        // ID-based detection above is the primary and most reliable signal.
        val textPhrases = listOf(
            "add a comment", "no comments yet", "view all comments", "top comments",
            "send to", "about this video", "about this channel", "chapters"
        )

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val rootCopy = AccessibilityNodeInfo.obtain(root)
        queue.add(rootCopy)
        var checked = 0
        var detected = false

        while (queue.isNotEmpty() && checked < 300) {
            val node = queue.removeFirst()
            checked++

            val id = node.viewIdResourceName?.lowercase() ?: ""
            if (idKeywords.any { id.contains(it) }) {
                detected = true
                break
            }

            val text = node.text?.toString()?.lowercase() ?: ""
            // Only match short UI-chrome strings (< 60 chars). Long strings are likely video
            // titles or captions that may legitimately contain words like "share" or "description".
            if (text.isNotEmpty() && text.length < 60 && textPhrases.any { text.contains(it) }) {
                detected = true
                break
            }

            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (contentDesc.isNotEmpty() && contentDesc.length < 60 && textPhrases.any { contentDesc.contains(it) }) {
                detected = true
                break
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            node.recycle()
        }

        while (queue.isNotEmpty()) {
            queue.removeFirst().recycle()
        }
        return detected
    }

    fun getVideoSignature(root: AccessibilityNodeInfo): String {
        val authorKeywords = listOf("author", "username", "nickname", "channel_name", "channel_title")
        val contentKeywords = listOf("caption", "description", "desc", "title")
        
        val collectedTexts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val rootCopy = AccessibilityNodeInfo.obtain(root)
        queue.add(rootCopy)
        
        var checked = 0
        while (queue.isNotEmpty() && checked < 200) {
            val node = queue.removeFirst()
            checked++
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val isAuthor = authorKeywords.any { id.contains(it) }
            val isContent = contentKeywords.any { id.contains(it) }
            val isComment = id.contains("comment") || id.contains("reply")
            val isControl = id.contains("player_control") || id.contains("progress") || id.contains("time")
            
            if ((isAuthor || isContent) && !isComment && !isControl) {
                node.text?.toString()?.trim()?.let { text ->
                    if (text.isNotEmpty() && text.length < 300) {
                        collectedTexts.add("$id:$text")
                    }
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            node.recycle()
        }
        while (queue.isNotEmpty()) {
            queue.removeFirst().recycle()
        }
        return if (collectedTexts.isNotEmpty()) collectedTexts.sorted().joinToString("||") else ""
    }

    private fun collectViewInfo(
        node: AccessibilityNodeInfo,
        outIds: MutableList<String>,
        outClasses: MutableList<String>,
        outTexts: MutableList<String>,
        maxDepth: Int = 8,
        depth: Int = 0
    ) {
        if (depth > maxDepth) return
        node.viewIdResourceName?.let(outIds::add)
        node.className?.toString()?.let(outClasses::add)
        node.text?.toString()?.takeIf { it.length < 100 }?.let(outTexts::add)
        for (index in 0 until node.childCount.coerceAtMost(20)) {
            val child = node.getChild(index)
            if (child != null) {
                try {
                    collectViewInfo(child, outIds, outClasses, outTexts, maxDepth, depth + 1)
                } finally {
                    child.recycle()
                }
            }
        }
    }

    /** Broader heuristic: check class names and text content for short-form indicators. */
    private fun heuristicShortVideo(root: AccessibilityNodeInfo, classes: String, texts: String): ContentType {
        // Check root properties
        if (root.isScrollable && (
                classes.contains("recyclerview") || classes.contains("listview") ||
                classes.contains("viewpager") || classes.contains("scrollview")
            )) {
            // Could be a feed — check for short-form terminology in text
            if (texts.contains("shorts") || texts.contains("reels") ||
                texts.contains("short") || texts.contains("scroll")) {
                return ContentType.SHORT
            }
            return ContentType.FEED
        }
        // Many scrolling container patterns
        if (classes.contains("shorts") || classes.contains("reel") ||
            classes.contains("story") || classes.contains("highlight")) {
            return ContentType.SHORT
        }
        return ContentType.UNKNOWN
    }
}

class ScrollDebouncer {
    private val lastCountedAt = mutableMapOf<String, Long>()

    fun shouldCount(key: String, nowMillis: Long, debounceMillis: Int): Boolean {
        val previous = lastCountedAt[key]
        if (previous == null) {
            lastCountedAt[key] = nowMillis
            return true
        }
        return if (nowMillis - previous >= debounceMillis) {
            lastCountedAt[key] = nowMillis
            true
        } else {
            false
        }
    }
}

interface Clock {
    fun now(): Long
}

object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
