// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Draggable scrollbar for a [LazyColumn]. Tap or drag the thumb to scroll. */
@Composable
fun LazyColumnScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 6.dp,
    minThumbHeight: Dp = 36.dp
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val minThumbPx = with(density) { minThumbHeight.toPx() }

    val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount.coerceAtLeast(1) } }
    val scrollFraction by remember {
        derivedStateOf {
            if (totalItems <= 1) 0f
            else listState.firstVisibleItemIndex.toFloat() / (totalItems - 1).coerceAtLeast(1)
        }
    }
    val visibleFraction by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount <= 0) 1f
            else info.visibleItemsInfo.size.toFloat() / info.totalItemsCount.toFloat()
        }
    }

    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var dragThumbOffsetPx by remember { mutableFloatStateOf(-1f) }
    val isDragging = dragThumbOffsetPx >= 0f

    val thumbHeightPx = if (trackHeightPx <= 0f) 0f
    else (trackHeightPx * visibleFraction).coerceAtLeast(minThumbPx)

    val maxThumbOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
    val thumbOffsetPx = if (isDragging) dragThumbOffsetPx else scrollFraction * maxThumbOffsetPx

    val alpha by animateFloatAsState(
        targetValue = if (listState.isScrollInProgress || isDragging) 1f else 0.5f,
        animationSpec = tween(180),
        label = "scrollbarAlpha"
    )

    fun scrollToFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val targetIndex = (clamped * (totalItems - 1)).roundToInt()
        scope.launch { listState.scrollToItem(targetIndex) }
    }

    Box(
        modifier = modifier
            .width(trackWidth + 16.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .onSizeChanged { trackHeightPx = it.height.toFloat() }
            .pointerInput(totalItems, trackHeightPx) {
                if (trackHeightPx <= 0f) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                    dragThumbOffsetPx = fraction * maxThumbOffsetPx
                    scrollToFraction(fraction)
                    dragThumbOffsetPx = -1f
                }
            }
            .pointerInput(totalItems, trackHeightPx, maxThumbOffsetPx) {
                if (trackHeightPx <= 0f) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragThumbOffsetPx = thumbOffsetPx },
                    onDragEnd = { dragThumbOffsetPx = -1f },
                    onDragCancel = { dragThumbOffsetPx = -1f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragThumbOffsetPx = (dragThumbOffsetPx + dragAmount.y).coerceIn(0f, maxThumbOffsetPx)
                        scrollToFraction(if (maxThumbOffsetPx > 0f) dragThumbOffsetPx / maxThumbOffsetPx else 0f)
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(trackWidth)
                .alpha(alpha)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    RoundedCornerShape(trackWidth / 2)
                )
        )
        if (trackHeightPx > 0f && thumbHeightPx > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                    .width(trackWidth)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .alpha(alpha)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        RoundedCornerShape(trackWidth / 2)
                    )
            )
        }
    }
}
