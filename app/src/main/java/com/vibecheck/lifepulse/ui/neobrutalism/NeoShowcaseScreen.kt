package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A demo screen wiring up the full Neobrutalism component set: a search field, filter chips,
 * a list of NeoCards, and primary/destructive NeoButtons.
 *
 * Wrap any usage in [NeoBrutalismTheme] so components pick up the right palette/typography.
 */
@Composable
fun NeoShowcaseScreen() {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Health", "Finance", "Focus")

    val items = remember {
        listOf(
            "Morning Run" to NeoColors.Lime,
            "Grocery Budget" to NeoColors.Yellow,
            "Read 20 Pages" to NeoColors.Cyan,
            "Meditate" to NeoColors.Purple
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoColors.Background)
            .padding(20.dp)
    ) {
        Text(
            text = "Today's Habits",
            style = NeoTypography.headlineLarge,
            color = NeoColors.OnSurface
        )

        Spacer(Modifier.height(16.dp))

        NeoTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search habits...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                NeoChip(
                    text = filter,
                    selected = filter == selectedFilter,
                    onClick = { selectedFilter = filter },
                    selectedColor = NeoColors.Accent
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items) { (label, color) ->
                NeoCard(
                    backgroundColor = color,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = label,
                            style = NeoTypography.titleLarge,
                            color = NeoColors.OnSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap to mark as complete",
                            style = NeoTypography.bodyMedium,
                            color = NeoColors.OnSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoButton(
                text = "Add Habit",
                onClick = { /* TODO */ },
                backgroundColor = NeoColors.Primary,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = NeoColors.OnPrimary
                    )
                },
                modifier = Modifier.weight(1f)
            )
            NeoButton(
                text = "Clear",
                onClick = { /* TODO */ },
                backgroundColor = NeoColors.Danger,
                contentColor = NeoColors.White,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = NeoColors.White
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun NeoShowcaseScreenPreview() {
    NeoBrutalismTheme {
        NeoShowcaseScreen()
    }
}


