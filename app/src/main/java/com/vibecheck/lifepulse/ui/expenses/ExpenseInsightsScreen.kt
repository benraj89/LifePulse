package com.vibecheck.lifepulse.ui.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.core.currentDateFlow
import com.vibecheck.lifepulse.ui.neobrutalism.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val chartColors = listOf(NeoColors.Yellow, NeoColors.Cyan, NeoColors.Coral, NeoColors.Lime, NeoColors.LightViolet, NeoColors.BubblegumPink)
private fun YearMonth.label() = format(DateTimeFormatter.ofPattern("MMM yyyy"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseInsightsScreen(state: ExpenseUiState, onMonth: (YearMonth) -> Unit,
                                   onAccount: (Long?) -> Unit, onBack: () -> Unit) {
    val today by remember { currentDateFlow() }.collectAsState(initial = LocalDate.now())
    var comparison by rememberSaveable(state.selectedMonth.toString()) { mutableStateOf(state.selectedMonth.minusMonths(1).toString()) }
    var matchDays by rememberSaveable { mutableStateOf(true) }
    var calendar by remember { mutableStateOf<String?>(null) }
    val comparedMonth = YearMonth.parse(comparison)
    val data by produceState<SpendingInsights?>(null, state.transactions, state.selectedMonth, comparedMonth,
        state.selectedAccountId, matchDays, today) {
        value = null
        value = withContext(Dispatchers.Default) {
            spendingInsights(state.transactions, state.selectedMonth, comparedMonth, state.selectedAccountId, matchDays, today)
        }
    }
    BackHandler(onBack = onBack)
    Scaffold(containerColor = NeoColors.Background, contentWindowInsets = WindowInsets(0), topBar = {
        TopAppBar(title = { Text("Spending insights", style = NeoTypography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to spending", Modifier.size(20.dp)) } },
            windowInsets = WindowInsets(0), colors = TopAppBarDefaults.topAppBarColors(containerColor = NeoColors.Background))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("spending_insights"),
            contentPadding = PaddingValues(16.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonthControl("MONTH", state.selectedMonth, NeoColors.Yellow, Modifier.weight(1f)) { calendar = "month" }
                    MonthControl("COMPARE WITH", comparedMonth, NeoColors.Cyan, Modifier.weight(1f)) { calendar = "compare" }
                }
                if (state.accounts.size > 1) NeoInlineChoice("Account",
                    listOf("all" to "All accounts") + state.accounts.map { it.id.toString() to it.name },
                    state.selectedAccountId?.toString() ?: "all", { onAccount(it.toLongOrNull()) }, Modifier.testTag("insights_account"))
                Row(Modifier.fillMaxWidth().clickable { matchDays = !matchDays }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(matchDays, { matchDays = it }, colors = CheckboxDefaults.colors(checkedColor = NeoColors.Ink, checkmarkColor = NeoColors.Lime))
                    Column(Modifier.weight(1f)) {
                        Text("Compare the same number of days", style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
                        Text("Turn off to compare full available months", style = NeoTypography.bodySmall)
                    }
                }
            }
            if (state.isLoading || data == null) item { Text("Loading your insights…") }
            else data?.let { insights ->
                item { ComparisonCard(insights) }
                item { CategoryChart(insights) }
                item { TrendChart(insights, onMonth) }
                item { CategoryChanges(insights) }
                item { Text("Spending charts include expenses only. Transfers, lending, borrowing, and repayments are excluded.", style = NeoTypography.bodySmall) }
            }
        }
    }
    calendar?.let { target ->
        NeoCalendarDialog(if (target == "month") state.selectedMonth else comparedMonth,
            { if (target == "month") onMonth(it) else comparison = it.toString() }, { calendar = null })
    }
}

@Composable
private fun MonthControl(title: String, month: YearMonth, color: Color, modifier: Modifier, onClick: () -> Unit) {
    NeoCard(modifier.clickable(onClick = onClick), backgroundColor = color, shape = RoundedCornerShape(4.dp),
        shadowOffsetX = 4.dp, shadowOffsetY = 4.dp, contentPadding = 10.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
            Text("${month.label()} ▾", style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun InsightCard(title: String, color: Color = NeoColors.Surface, content: @Composable ColumnScope.() -> Unit) {
    NeoCard(Modifier.fillMaxWidth().padding(bottom = 4.dp), backgroundColor = color, shape = RoundedCornerShape(4.dp), contentPadding = 3.dp) {
        Column {
            Row(Modifier.fillMaxWidth().background(NeoColors.Ink).padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = NeoTypography.labelLarge, color = NeoColors.White, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("///", style = NeoTypography.titleMedium, color = NeoColors.Lime, fontWeight = FontWeight.Black)
            }
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun ComparisonCard(data: SpendingInsights) {
    InsightCard("01 / THE BIG PICTURE", NeoColors.Yellow) {
        Text(Money.format(data.spent), style = NeoTypography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Spent in ${data.month.label()}${if (data.days in 1 until data.month.lengthOfMonth()) " · through day ${data.days}" else ""}", style = NeoTypography.bodySmall)
        val difference = data.comparisonSpent - data.previousSpent
        val change = when {
            data.month == data.comparedMonth -> "Same month selected"
            data.previousSpent == 0L && data.comparisonSpent == 0L -> "No spending in either period"
            data.previousSpent == 0L -> "No spending in the comparison period"
            difference == 0L -> "Spending stayed the same"
            else -> "${String.format(Locale.getDefault(), "%.1f", abs(data.changePercent!!))}% ${if (difference > 0) "more" else "less"} · ${Money.format(abs(difference))}"
        }
        Text(change, style = NeoTypography.labelLarge, fontWeight = FontWeight.Black,
            modifier = Modifier.background(if (difference > 0) NeoColors.Coral else NeoColors.MintGreen)
                .border(2.dp, NeoColors.Border).padding(8.dp))
        val maximum = maxOf(data.comparisonSpent, data.previousSpent, 1L)
        ComparisonBar(data.month, data.comparisonDays, data.comparisonSpent, maximum, NeoColors.Ink)
        ComparisonBar(data.comparedMonth, data.comparedDays, data.previousSpent, maximum, NeoColors.Cyan)
        HorizontalDivider(thickness = 2.dp, color = NeoColors.Border)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat("Daily average", Money.format(data.averagePerDay), Modifier.weight(1f))
            SmallStat("Expenses", data.count.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat("Income", Money.format(data.income), Modifier.weight(1f))
            SmallStat("Income − spending", Money.format(data.income - data.spent), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = NeoTypography.bodySmall)
        Text(value, style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ComparisonBar(month: YearMonth, days: Int, amount: Long, maximum: Long, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${month.label()} · ${if (days == 0) "no elapsed days" else "1–$days"}", style = NeoTypography.bodySmall, modifier = Modifier.weight(1f))
            Text(Money.format(amount), style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Box(Modifier.fillMaxWidth().height(14.dp).background(NeoColors.Surface).border(2.dp, NeoColors.Border)) {
            if (amount > 0) Box(Modifier.fillMaxWidth((amount.toDouble() / maximum).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color))
        }
    }
}

@Composable
internal fun CategoryChart(data: SpendingInsights) {
    var selectedKey by rememberSaveable(data.month.toString(), data.categories) { mutableStateOf<String?>(null) }
    var showAll by rememberSaveable(data.month.toString()) { mutableStateOf(false) }
    val slices = data.slices
    val active = slices.firstOrNull { it.key == selectedKey }
    InsightCard("02 / WHERE IT WENT") {
        if (data.spent == 0L) {
            Text("No expenses for this month", style = NeoTypography.titleMedium)
            Text("Pick another month or add an expense to see your category chart.", style = NeoTypography.bodySmall)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Spending by category. Exact values are listed below." }) {
                        val diameter = size.minDimension - 12.dp.toPx()
                        val origin = Offset(4.dp.toPx(), 4.dp.toPx())
                        drawCircle(NeoColors.Ink, diameter / 2, center = Offset(size.width / 2 + 3.dp.toPx(), size.height / 2 + 3.dp.toPx()))
                        var start = -90f
                        slices.forEachIndexed { index, slice ->
                            val sweep = (slice.amount.toDouble() / data.spent * 360).toFloat()
                            val color = chartColors[index % chartColors.size].let { if (active != null && active.key != slice.key) lerp(it, NeoColors.Surface, 0.7f) else it }
                            drawArc(color, start, sweep, true, origin, Size(diameter, diameter))
                            drawArc(NeoColors.Ink, start, sweep, true, origin, Size(diameter, diameter), style = Stroke(2.dp.toPx()))
                            start += sweep
                        }
                        val center = origin + Offset(diameter / 2, diameter / 2)
                        drawCircle(NeoColors.Surface, diameter * 0.31f, center)
                        drawCircle(NeoColors.Ink, diameter * 0.31f, center, style = Stroke(3.dp.toPx()))
                    }
                    Column(Modifier.width(115.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (active == null) "SPENDING" else "${(active.amount.toDouble() / data.spent * 100).roundToInt()}%",
                            style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
                        Text(if (active == null) "${data.categories.size} categories" else active.name,
                            style = NeoTypography.labelLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            Text("Tap a category to highlight it", style = NeoTypography.bodySmall)
            slices.forEachIndexed { index, slice ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("insight_slice_${slice.key}")
                    .semantics { selected = selectedKey == slice.key }
                    .background(if (selectedKey == slice.key) NeoColors.Concrete else NeoColors.Surface)
                    .clickable { selectedKey = if (selectedKey == slice.key) null else slice.key }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(14.dp).background(chartColors[index % chartColors.size]).border(2.dp, NeoColors.Border))
                    Text(slice.name, style = NeoTypography.bodySmall, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 160.dp)) {
                        Text(Money.format(slice.amount), style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
                        Text("${String.format(Locale.getDefault(), "%.1f", slice.amount.toDouble() / data.spent * 100)}%", style = NeoTypography.bodySmall)
                    }
                }
            }
            if (data.categories.size > 6) {
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Hide full breakdown" else "All ${data.categories.size} categories", color = NeoColors.OnSurface) }
                if (showAll) data.categories.forEach { category ->
                    SmallStat(category.name, Money.format(category.amount), Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun TrendChart(data: SpendingInsights, onMonth: (YearMonth) -> Unit) {
    InsightCard("03 / SIX-MONTH TREND") {
        val maximum = data.trend.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
        Text(if (data.trend.all { it.amount == 0L }) "No spending in these six months" else "Highest month: ${Money.format(maximum)}",
            style = NeoTypography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            data.trend.forEach { point ->
                val isSelected = point.month == data.month
                Column(Modifier.weight(1f).testTag("insight_month_${point.month}")
                    .semantics { contentDescription = "${point.month.label()}, ${Money.format(point.amount)} spent"; selected = isSelected }
                    .clickable { onMonth(point.month) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(135.dp), contentAlignment = Alignment.BottomCenter) {
                        if (point.amount > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 3.dp)
                            .height((point.amount.toDouble() / maximum * 125).toFloat().dp.coerceAtLeast(3.dp))
                            .neoHardShadow(shape = RoundedCornerShape(0.dp), offsetX = 3.dp, offsetY = 3.dp)
                            .background(if (isSelected) NeoColors.Cyan else NeoColors.Yellow).border(2.dp, NeoColors.Border))
                        else Box(Modifier.fillMaxWidth().height(2.dp).background(NeoColors.Border))
                    }
                    Text(point.month.format(DateTimeFormatter.ofPattern("MMM")), style = NeoTypography.labelSmall,
                        fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
                    Text(point.month.format(DateTimeFormatter.ofPattern("yy")), style = NeoTypography.bodySmall)
                }
            }
        }
        Text("Tap a bar to explore that month. The current month includes spending so far.", style = NeoTypography.bodySmall)
    }
}

@Composable
private fun CategoryChanges(data: SpendingInsights) {
    InsightCard("04 / WHAT CHANGED?") {
        Text("${data.month.label()} vs ${data.comparedMonth.label()} · same periods as above", style = NeoTypography.bodySmall)
        if (data.changes.isEmpty()) Text("Your category changes will appear here.", style = NeoTypography.bodySmall)
        data.changes.take(5).forEach { category ->
            val difference = category.amount - category.previous
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(category.name, style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
                    Text("${Money.format(category.previous)} → ${Money.format(category.amount)}", style = NeoTypography.bodySmall)
                }
                Text(when {
                    difference == 0L -> "No change"
                    category.previous == 0L -> "New"
                    else -> (if (difference > 0) "+" else "−") + Money.format(abs(difference))
                }, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                    modifier = Modifier.widthIn(max = 140.dp).background(if (difference > 0) NeoColors.Coral else NeoColors.MintGreen)
                        .border(2.dp, NeoColors.Border).padding(6.dp))
            }
        }
        if (data.changes.size > 5) Text("Showing the five biggest changes", style = NeoTypography.bodySmall)
    }
}
