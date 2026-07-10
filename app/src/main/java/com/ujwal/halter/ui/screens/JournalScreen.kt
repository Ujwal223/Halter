// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.JournalEntry
import com.ujwal.halter.data.JournalReason
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Reason metadata ───────────────────────────────────────────────────────────

private data class ReasonMeta(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
private fun reasonMeta(reason: JournalReason): ReasonMeta = when (reason) {
    JournalReason.BOREDOM -> ReasonMeta(
        label = "Boredom",
        icon = Icons.Outlined.SentimentDissatisfied,
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )
    JournalReason.HABIT -> ReasonMeta(
        label = "Habit",
        icon = Icons.Outlined.Autorenew,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    JournalReason.ACTUAL_NEED -> ReasonMeta(
        label = "Actual Need",
        icon = Icons.Outlined.CheckCircle,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    )
    JournalReason.NOTIFICATION -> ReasonMeta(
        label = "Notification",
        icon = Icons.Outlined.Notifications,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

// ── Time helpers ─────────────────────────────────────────────────────────────

private fun groupLabel(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val entryDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when {
        entryDate == today -> "Today"
        entryDate == today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()).format(entryDate)
    }
}

private fun formatTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

// ── Stats header card ─────────────────────────────────────────────────────────

@Composable
private fun StatsCard(entries: List<JournalEntry>) {
    val total = entries.size
    val topReason = entries
        .groupBy { it.reason }
        .maxByOrNull { it.value.size }
        ?.key

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$total entries",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "in your journal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (topReason != null) {
                val meta = reasonMeta(topReason)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Most common",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = meta.containerColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                meta.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = meta.contentColor
                            )
                            Text(
                                meta.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = meta.contentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Reason filter chips ───────────────────────────────────────────────────────

@Composable
private fun ReasonFilterRow(
    selected: JournalReason?,
    onSelect: (JournalReason?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        JournalReason.entries.forEach { reason ->
            val meta = reasonMeta(reason)
            FilterChip(
                selected = selected == reason,
                onClick = { onSelect(if (selected == reason) null else reason) },
                label = { Text(meta.label) },
                leadingIcon = {
                    Icon(
                        meta.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ── Entry card ────────────────────────────────────────────────────────────────

@Composable
private fun JournalEntryCard(entry: JournalEntry) {
    val context = LocalContext.current
    val appName = remember(entry.packageName) {
        try {
            context.packageManager
                .getApplicationInfo(entry.packageName, 0)
                .loadLabel(context.packageManager)
                .toString()
        } catch (_: Exception) { entry.packageName }
    }
    val meta = reasonMeta(entry.reason)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon with subtle background
            Box(contentAlignment = Alignment.Center) {
                AppIcon(packageName = entry.packageName, modifier = Modifier.size(44.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // App name + time in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formatTime(entry.timestampEpochMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reason badge with icon
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = meta.containerColor,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            meta.icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = meta.contentColor
                        )
                        Text(
                            text = meta.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = meta.contentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(filterActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Book,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                if (filterActive) "No entries match this filter" else "Your journal is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                if (filterActive)
                    "Try selecting a different reason or view all entries."
                else
                    "When you open a monitored app, you'll be asked why you're opening it.\nYour reflections appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!filterActive) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Enable Journal in Settings → Journal toggle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onBack: (() -> Unit)? = null) {
    val repository: HalterRepository = koinInject()
    val entries by repository.observeJournal().collectAsState(initial = emptyList())
    var selectedReason by remember { mutableStateOf<JournalReason?>(null) }

    val sortedEntries = remember(entries) {
        entries.sortedByDescending { it.timestampEpochMillis }
    }

    val filteredEntries = remember(sortedEntries, selectedReason) {
        if (selectedReason == null) sortedEntries
        else sortedEntries.filter { it.reason == selectedReason }
    }

    val grouped = remember(filteredEntries) {
        filteredEntries
            .groupBy { groupLabel(it.timestampEpochMillis) }
            .entries
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyState(filterActive = false)
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Stats header
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCard(sortedEntries)
                        ReasonFilterRow(
                            selected = selectedReason,
                            onSelect = { selectedReason = it }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (filteredEntries.isEmpty()) {
                    item { EmptyState(filterActive = true) }
                } else {
                    grouped.forEach { (dateLabel, dayEntries) ->
                        // Date section header
                        item(key = "header_$dateLabel") {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 12.dp, bottom = 6.dp)
                            )
                        }
                        // Entry cards
                        items(dayEntries, key = { it.id }) { entry ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(180))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                                    JournalEntryCard(entry)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}
