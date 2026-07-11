package com.ujwal.halter.service

data class FeedSignature(
    val packageName: String,
    val feedIndicatorIds: List<String>,
    val exclusionIds: List<String> = emptyList()
)

object FeedScreenSignatures {
    val SUPPORTED = listOf(
        FeedSignature(
            packageName = "com.instagram.android",
            feedIndicatorIds = listOf(
                "com.instagram.android:id/clips_viewer_view_pager",
                "com.instagram.android:id/reels_tray_container"
            ),
            exclusionIds = listOf(
                "com.instagram.android:id/direct_inbox_container",
                "com.instagram.android:id/comments_bottom_sheet"
            )
        ),
        FeedSignature(
            packageName = "com.google.android.youtube",
            feedIndicatorIds = listOf(
                "com.google.android.youtube:id/reel_player_page_container",
                "com.google.android.youtube:id/reel_recycler"
            )
        ),
        FeedSignature(
            packageName = "com.zhiliaoapp.musically",
            feedIndicatorIds = listOf(
                "com.zhiliaoapp.musically:id/feed_container",
                "com.zhiliaoapp.musically:id/fragment_feed"
            )
        ),
        FeedSignature(
            packageName = "com.ss.android.ugc.trill",
            feedIndicatorIds = listOf("com.ss.android.ugc.trill:id/feed_container")
        ),
        FeedSignature(
            packageName = "com.snapchat.android",
            feedIndicatorIds = listOf("com.snapchat.android:id/spotlight_feed_recycler")
        )
    )

    fun forPackage(pkg: String): FeedSignature? = SUPPORTED.find { it.packageName == pkg }
}
