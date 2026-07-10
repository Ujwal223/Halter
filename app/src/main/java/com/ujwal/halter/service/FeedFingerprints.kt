// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.view.accessibility.AccessibilityNodeInfo

/** Per-app view-id fingerprints for feed detection and blocking overlays. Version separately from detection logic. */
object FeedFingerprints {

    val WATCHED_PACKAGES: Set<String> = KnownScrollApps.all

    /** Resource-id substrings that indicate we ARE inside a scrollable reel/short feed. */
    val FEED_CONTAINER_IDS = mapOf(
        // Instagram: clips_tab (selected) is the highest-confidence Reels signal;
        // clips_viewer_view_pager / reel_viewer_view_pager are the feed container IDs.
        "com.instagram.android" to listOf(
            "clips_tab", "clips_viewer_view_pager", "reel_viewer_view_pager", "clips_viewer"
        ),
        // YouTube: reel_progress_bar is present ONLY on the Shorts player (from Shorts-Blocker reference).
        "com.google.android.youtube" to listOf(
            "reel_progress_bar", "reel_recycler", "reel_player_page_container", "reel_player", "shorts_player", "shorts_container"
        ),
        "app.revanced.android.youtube" to listOf(
            "reel_progress_bar", "reel_recycler", "reel_player_page_container", "reel_player", "shorts_player", "shorts_container"
        ),
        "app.rvx.android.youtube" to listOf(
            "reel_progress_bar", "reel_recycler", "reel_player_page_container", "reel_player", "shorts_player", "shorts_container"
        ),
        "com.snapchat.android" to listOf("spotlight", "discover_feed", "snap_map"),
        "com.zhiliaoapp.musically" to listOf(
            // Obfuscated IDs change per release; cast a wide net
            "feed_container", "viewpager", "video_feed",
            "aweme_video_container", "main_surface_stub",
            "v_aweme_cover", "recycler_view"
        ),
        "com.ss.android.ugc.trill" to listOf(
            "feed_container", "viewpager", "video_feed",
            "aweme_video_container", "main_surface_stub",
            "v_aweme_cover", "recycler_view"
        ),
        "com.facebook.katana" to listOf("reels", "reel_viewer", "reels_tab"),
        "com.pinterest" to listOf("story_pin", "closeup"),
        "com.twitter.android" to listOf("video_player", "media_container")
    )

    /** Resource-id substrings that indicate a blocking overlay is open. Suppresses all signal registration. */
    val BLOCKING_OVERLAY_IDS = mapOf(
        "com.instagram.android" to listOf(
            // Comment & reply panels
            "comment_bottom_sheet", "comments_recycler", "comments_header",
            "comment_text_input", "reply_bottom_sheet",
            // Share & send
            "share_sheet", "direct_share", "reshare_tray",
            // Like/interaction lists
            "like_list_container", "avatar_stack_row", "likers_bottom_sheet",
            // Profile & about drawers
            "profile_header", "profile_root_layout", "channel_info",
            // Search overlay
            "action_bar_search_results", "search_results_container",
            // Audio / music panels
            "audio_detail_bottom_sheet",
            // Context menus
            "bottom_sheet_root", "bottom_sheet_container"
        ),
        "com.google.android.youtube" to listOf(
            // Comments
            "comments_bottom_sheet", "comments_list", "comment_input_text",
            // Info / description panels (engagement_panel covers chapters, description, transcript)
            "engagement_panel", "engagement_panel_content_container",
            "video_description_bottom_sheet",
            // Share
            "share_sheet", "share_content_list",
            // Context / overflow menus
            "menu_bottom_sheet", "contextual_menu_container",
            // Search
            "search_container", "search_suggest_recycler"
        ),
        "app.revanced.android.youtube" to listOf(
            "comments_bottom_sheet", "engagement_panel", "engagement_panel_content_container",
            "share_sheet", "menu_bottom_sheet", "video_description_bottom_sheet",
            "search_container"
        ),
        "app.rvx.android.youtube" to listOf(
            "comments_bottom_sheet", "engagement_panel", "engagement_panel_content_container",
            "share_sheet", "menu_bottom_sheet", "video_description_bottom_sheet",
            "search_container"
        ),
        "com.snapchat.android" to listOf(
            "comment", "reply", "share_sheet", "send_to",
            "story_metadata_bottom_sheet", "profile_surface",
            "chat_input", "search_input"
        ),
        "com.zhiliaoapp.musically" to listOf(
            // TikTok comment & share
            "comment_list_container", "comment_keyboard_container", "comment_text_input", "comment_reply",
            "share_bottom_sheet", "direct_share", "share_container", "share_panel",
            // TikTok description / creator panel
            "description_panel", "expanded_description", "video_description",
            "music_panel",
            "search_panel", "search_input", "search_bar", "search_result",
            // Following / follower lists
            "follow_list", "fans_list"
        ),
        "com.ss.android.ugc.trill" to listOf(
            // TikTok comment & share
            "comment_list_container", "comment_keyboard_container", "comment_text_input", "comment_reply",
            "share_bottom_sheet", "direct_share", "share_container", "share_panel",
            // TikTok description / creator panel
            "description_panel", "expanded_description", "video_description",
            "music_panel",
            "search_panel", "search_input", "search_bar", "search_result",
            // Following / follower lists
            "follow_list", "fans_list"
        ),
        "com.facebook.katana" to listOf(
            "comment", "comments_list", "reply_composer",
            "share_sheet", "composer",
            "profile_card", "group_header",
            "search_input"
        ),
        "com.pinterest" to listOf(
            "comment", "comment_input",
            "share", "pin_detail", "story_about",
            "search_bar"
        ),
        "com.twitter.android" to listOf(
            "reply", "compose",
            "share", "more_options",
            "user_profile", "search_input",
            "context_menu"
        )
    )

    /**
     * Activity class-name substrings that indicate a feed is showing.
     * Used as fallback when resource IDs are obfuscated (TikTok, Snapchat).
     */
    val FEED_ACTIVITY_NAMES: Map<String, List<String>> = mapOf(
        "com.zhiliaoapp.musically" to listOf("aweme.main.MainActivity", "aweme.feed"),
        "com.ss.android.ugc.trill"  to listOf("aweme.main.MainActivity", "aweme.feed"),
        "com.snapchat.android"       to listOf("LandingPageActivity", "FeedActivity"),
    )

    fun isFeedScreenActive(
        root: AccessibilityNodeInfo,
        pkg: String,
        windowClassName: String? = null
    ): Boolean {
        // Primary: fast resource-ID search using platform index
        val ids = FEED_CONTAINER_IDS[pkg]
        if (ids != null) {
            if (hasNodeWithIdSubstring(root, pkg, ids)) return true
        }
        // Secondary: activity class-name match (reliable for obfuscated apps)
        val feedActivities = FEED_ACTIVITY_NAMES[pkg]
        if (windowClassName != null && feedActivities != null) {
            if (feedActivities.any { windowClassName.contains(it, ignoreCase = true) }) {
                return true
            }
        }
        return false
    }

    fun isBlockingOverlayPresent(root: AccessibilityNodeInfo, pkg: String): Boolean {
        val ids = BLOCKING_OVERLAY_IDS[pkg] ?: return false
        return hasNodeWithIdSubstring(root, pkg, ids)
    }

    fun hasNodeWithIdSubstring(root: AccessibilityNodeInfo, pkg: String, substrings: List<String>): Boolean {
        for (sub in substrings) {
            val fullId = "$pkg:id/$sub"
            val list = root.findAccessibilityNodeInfosByViewId(fullId)
            if (list.isNotEmpty()) {
                list.forEach { it.recycle() }
                return true
            }
        }
        // Fallback: BFS traversal in case package name prefix is different or missing
        val node = findNodeByIdSubstring(root, substrings)
        if (node != null) {
            node.recycle()
            return true
        }
        return false
    }

    fun findNodeByIdSubstring(root: AccessibilityNodeInfo, substrings: List<String>): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val rootCopy = AccessibilityNodeInfo.obtain(root)
        queue.add(rootCopy)
        var checked = 0
        var foundNode: AccessibilityNodeInfo? = null
        while (queue.isNotEmpty() && checked < 400) {
            val node = queue.removeFirst()
            checked++
            val id = node.viewIdResourceName
            if (id != null && substrings.any { id.contains(it, ignoreCase = true) }) {
                foundNode = node
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
        return foundNode
    }
}
