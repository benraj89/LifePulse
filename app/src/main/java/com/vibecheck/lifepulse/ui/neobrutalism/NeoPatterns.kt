package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R

/** Header actions move below the title when text needs more room. */
@Composable
fun NeoPageHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (maxWidth < 290.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ComicPageTitle(title)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicPageTitle(title, Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
private fun ComicPageTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, style = NeoTypography.headlineMedium, color = NeoColors.Ink,
        modifier = modifier.semantics { heading() }.drawBehind {
            drawRect(NeoColors.Yellow, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 7.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width.coerceAtMost(100.dp.toPx()), 7.dp.toPx()))
        })
}

@Composable
fun NeoFeatureCard(title: String, color: Color, modifier: Modifier = Modifier,
                   headerAction: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    NeoCard(modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), backgroundColor = color,
        borderWidth = 2.dp, shadowOffsetX = 4.dp, shadowOffsetY = 4.dp, contentPadding = 0.dp) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).semantics { heading() }) {
                    NeoSticker(title, color = NeoColors.Surface)
                }
                if (headerAction != null) headerAction() else NeoBurst(color = NeoColors.Yellow)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}

/** Exact amounts stay intact. A label/value pair stacks when it cannot fit on one line. */
@Composable
fun NeoValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val required = measurer.measure(AnnotatedString(label), NeoTypography.bodySmall, softWrap = false).size.width +
            measurer.measure(AnnotatedString(value), NeoTypography.titleMedium, softWrap = false).size.width + gap
        if (required <= constraints.maxWidth) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = NeoTypography.bodySmall, color = NeoColors.SecondaryText, modifier = Modifier.weight(1f))
            Text(value, style = NeoTypography.titleMedium, color = NeoColors.Ink)
        } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = NeoTypography.bodySmall, color = NeoColors.SecondaryText)
            Text(value, style = NeoTypography.titleMedium, color = NeoColors.Ink, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun NeoSheetHeader(title: String, onClose: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(title, style = NeoTypography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
        IconButton(onClick = onClose, enabled = enabled) {
            Icon(Icons.Default.Close, stringResource(R.string.transaction_close))
        }
    }
}

@Preview(name = "Shared feature and currency rows", widthDp = 360, showBackground = true)
@Preview(name = "Shared feature • large text", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun NeoPatternsPreview() {
    NeoBrutalismTheme {
        Surface(color = NeoColors.Background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NeoFeatureCard("Your money", NeoColors.Expense) {
                    NeoValueRow("Account balance", "$123,456,789,012.34")
                }
                NeoButton("Add entry", {}, Modifier.fillMaxWidth())
            }
        }
    }
}
