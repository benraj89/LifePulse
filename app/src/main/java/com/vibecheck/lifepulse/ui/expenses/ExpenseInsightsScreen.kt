package com.vibecheck.lifepulse.ui.expenses

import com.vibecheck.lifepulse.R

import androidx.compose.ui.res.stringResource

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
import androidx.compose.ui.res.pluralStringResource
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
import kotlin.math.abs
import kotlin.math.roundToInt

private val chartColors = listOf(NeoColors.Coral, NeoColors.SkyBlue, NeoColors.Yellow, NeoColors.LightViolet, NeoColors.BubblegumPink, NeoColors.Lime)
private fun categoryColor(key: String) = chartColors[Math.floorMod(key.hashCode(), chartColors.size)]
private fun YearMonth.label() = format(DateTimeFormatter.ofPattern("MMM yyyy"))

@Composable
private fun CategoryInsight.displayName() = if (isOther) stringResource(R.string.insights_other_categories) else name

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
        TopAppBar(title = { Text(stringResource(R.string.insights_title), style = NeoTypography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.insights_back), Modifier.size(20.dp)) } },
            windowInsets = WindowInsets(0), colors = TopAppBarDefaults.topAppBarColors(containerColor = NeoColors.Background))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxSize().testTag("spending_insights"),
            contentPadding = PaddingValues(12.dp, 6.dp, 12.dp, 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonthControl(stringResource(R.string.insights_month), state.selectedMonth, NeoColors.Yellow, Modifier.weight(1f)) { calendar = "month" }
                    MonthControl(stringResource(R.string.insights_compare_with), comparedMonth, NeoColors.LightViolet, Modifier.weight(1f)) { calendar = "compare" }
                }
                if (state.accounts.size > 1) NeoInlineChoice(stringResource(R.string.account_label),
                    listOf("all" to stringResource(R.string.finance_all_accounts)) + state.accounts.map { it.id.toString() to it.name },
                    state.selectedAccountId?.toString() ?: "all", { onAccount(it.toLongOrNull()) }, Modifier.testTag("insights_account"))
                Row(Modifier.fillMaxWidth().clickable { matchDays = !matchDays }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(matchDays, { matchDays = it }, colors = CheckboxDefaults.colors(checkedColor = NeoColors.Ink, checkmarkColor = NeoColors.Surface))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.insights_match_days), style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.insights_match_days_hint), style = NeoTypography.bodySmall)
                    }
                }
            }
            if (state.isLoading || data == null) item { Text(stringResource(R.string.insights_loading)) }
            else data?.let { insights ->
                item { ComparisonCard(insights) }
                item { CategoryChart(insights) }
                item { TrendChart(insights, onMonth) }
                item { CategoryChanges(insights) }
                item { Text(stringResource(R.string.insights_expenses_only), style = NeoTypography.bodySmall) }
            }
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
        shadowOffsetX = 0.dp, shadowOffsetY = 0.dp, contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
            Text("${month.label()} ▾", style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun InsightCard(title: String, color: Color = NeoColors.Surface, content: @Composable ColumnScope.() -> Unit) {
    NeoFeatureCard(title, color, content = content)
}

@Composable
private fun ComparisonCard(data: SpendingInsights) {
    InsightCard(stringResource(R.string.insights_big_picture), NeoColors.Expense) {
        Text(Money.format(data.spent), style = NeoTypography.headlineLarge, modifier = Modifier.fillMaxWidth())
        Text(if (data.days in 1 until data.month.lengthOfMonth()) stringResource(R.string.insights_spent_through_day, data.month.label(), data.days)
            else stringResource(R.string.insights_spent_month, data.month.label()), style = NeoTypography.bodySmall)
        val difference = data.comparisonSpent - data.previousSpent
        val change = when {
            data.month == data.comparedMonth -> stringResource(R.string.insights_same_month)
            data.previousSpent == 0L && data.comparisonSpent == 0L -> stringResource(R.string.insights_no_spending_either)
            data.previousSpent == 0L -> stringResource(R.string.insights_no_comparison_spending)
            difference == 0L -> stringResource(R.string.insights_spending_unchanged)
            else -> stringResource(if (difference > 0) R.string.insights_change_more else R.string.insights_change_less,
                abs(data.changePercent!!), Money.format(abs(difference)))
        }
        Text(change, style = NeoTypography.labelLarge, fontWeight = FontWeight.Black,
            modifier = Modifier.background(when {
                data.month == data.comparedMonth || data.previousSpent == 0L || difference == 0L -> NeoColors.Concrete
                difference > 0 -> NeoColors.Coral
                else -> NeoColors.Success
            }).padding(8.dp))
        if (data.spent == 0L && data.previousSpent == 0L) return@InsightCard
        val maximum = maxOf(data.comparisonSpent, data.previousSpent, 1L)
        ComparisonBar(data.month, data.comparisonDays, data.comparisonSpent, maximum, NeoColors.Ink)
        ComparisonBar(data.comparedMonth, data.comparedDays, data.previousSpent, maximum, NeoColors.Concrete)
        HorizontalDivider(thickness = 2.dp, color = NeoColors.Border)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat(stringResource(R.string.insights_daily_average), Money.format(data.averagePerDay), Modifier.weight(1f))
            SmallStat(stringResource(R.string.expense_title), data.count.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat(stringResource(R.string.transaction_type_income), Money.format(data.income), Modifier.weight(1f))
            SmallStat(stringResource(R.string.insights_income_minus_spending), Money.format(data.income - data.spent), Modifier.weight(1f))
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
            Text(if (days == 0) stringResource(R.string.insights_no_elapsed_days, month.label()) else stringResource(R.string.insights_day_range, month.label(), days),
                style = NeoTypography.bodySmall, modifier = Modifier.weight(1f))
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
    InsightCard(stringResource(R.string.insights_categories_heading)) {
        if (data.spent == 0L) {
            Text(stringResource(R.string.insights_empty_month), style = NeoTypography.titleMedium)
            Text(stringResource(R.string.insights_empty_month_hint), style = NeoTypography.bodySmall)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
                    val chartDescription = stringResource(R.string.insights_category_chart_description)
                    Canvas(Modifier.fillMaxSize().semantics { contentDescription = chartDescription }) {
                        val diameter = size.minDimension - 12.dp.toPx()
                        val origin = Offset(4.dp.toPx(), 4.dp.toPx())
                        drawCircle(NeoColors.Ink, diameter / 2, center = Offset(size.width / 2 + 3.dp.toPx(), size.height / 2 + 3.dp.toPx()))
                        var start = -90f
                        slices.forEachIndexed { index, slice ->
                            val sweep = (slice.amount.toDouble() / data.spent * 360).toFloat()
                            val color = categoryColor(slice.key).let { if (active != null && active.key != slice.key) lerp(it, NeoColors.Surface, 0.7f) else it }
                            drawArc(color, start, sweep, true, origin, Size(diameter, diameter))
                            drawArc(NeoColors.Ink, start, sweep, true, origin, Size(diameter, diameter), style = Stroke(2.dp.toPx()))
                            start += sweep
                        }
                        val center = origin + Offset(diameter / 2, diameter / 2)
                        drawCircle(NeoColors.Surface, diameter * 0.31f, center)
                        drawCircle(NeoColors.Ink, diameter * 0.31f, center, style = Stroke(3.dp.toPx()))
                    }
                    Column(Modifier.width(115.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (active == null) stringResource(R.string.insights_spending_label) else stringResource(R.string.insights_percent_whole, (active.amount.toDouble() / data.spent * 100).roundToInt()),
                            style = NeoTypography.labelSmall, fontWeight = FontWeight.Black)
                        Text(if (active == null) pluralStringResource(R.plurals.insights_category_count, data.categories.size, data.categories.size) else active.displayName(),
                            style = NeoTypography.labelLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            Text(stringResource(R.string.insights_highlight_hint), style = NeoTypography.bodySmall)
            slices.forEachIndexed { index, slice ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("insight_slice_${slice.key}")
                    .semantics { selected = selectedKey == slice.key }
                    .background(if (selectedKey == slice.key) NeoColors.Concrete else NeoColors.Surface)
                    .clickable { selectedKey = if (selectedKey == slice.key) null else slice.key }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(14.dp).background(categoryColor(slice.key)).border(1.dp, NeoColors.Border))
                    Text(slice.displayName(), style = NeoTypography.bodySmall, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 160.dp)) {
                        Text(Money.format(slice.amount), style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.insights_percent_decimal, slice.amount.toDouble() / data.spent * 100), style = NeoTypography.bodySmall)
                    }
                }
            }
            if (data.categories.size > 6) {
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) stringResource(R.string.insights_hide_breakdown) else pluralStringResource(R.plurals.insights_all_categories, data.categories.size, data.categories.size), color = NeoColors.OnSurface) }
                if (showAll) data.categories.forEach { category ->
                    SmallStat(category.name, Money.format(category.amount), Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun TrendChart(data: SpendingInsights, onMonth: (YearMonth) -> Unit) {
    InsightCard(stringResource(R.string.insights_trend_heading)) {
        val maximum = data.trend.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
        Text(if (data.trend.all { it.amount == 0L }) stringResource(R.string.insights_empty_trend) else stringResource(R.string.insights_highest_month, Money.format(maximum)),
            style = NeoTypography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            data.trend.forEach { point ->
                val isSelected = point.month == data.month
                val monthDescription = stringResource(R.string.insights_month_spending_description, point.month.label(), Money.format(point.amount))
                Column(Modifier.weight(1f).testTag("insight_month_${point.month}")
                    .semantics { contentDescription = monthDescription; selected = isSelected }
                    .clickable { onMonth(point.month) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(if (data.trend.all { it.amount == 0L }) 24.dp else 135.dp), contentAlignment = Alignment.BottomCenter) {
                        if (point.amount > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 3.dp)
                            .height((point.amount.toDouble() / maximum * 125).toFloat().dp.coerceAtLeast(3.dp))
                            .background(if (isSelected) NeoColors.Expense else NeoColors.Concrete).border(1.dp, NeoColors.Border))
                        else Box(Modifier.fillMaxWidth().height(2.dp).background(NeoColors.Border))
                    }
                    Text(point.month.format(DateTimeFormatter.ofPattern("MMM")), style = NeoTypography.labelSmall,
                        fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
                    Text(point.month.format(DateTimeFormatter.ofPattern("yy")), style = NeoTypography.bodySmall)
                }
            }
        }
        Text(stringResource(R.string.insights_trend_hint), style = NeoTypography.bodySmall)
    }
}

@Composable
private fun CategoryChanges(data: SpendingInsights) {
    InsightCard(stringResource(R.string.insights_changes_heading)) {
        Text(stringResource(R.string.insights_comparison_periods, data.month.label(), data.comparedMonth.label()), style = NeoTypography.bodySmall)
        if (data.changes.isEmpty()) Text(stringResource(R.string.insights_empty_changes), style = NeoTypography.bodySmall)
        data.changes.take(5).forEach { category ->
            val difference = category.amount - category.previous
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(category.name, style = NeoTypography.labelLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.insights_amount_change, Money.format(category.previous), Money.format(category.amount)), style = NeoTypography.bodySmall)
                }
                Text(when {
                    difference == 0L -> stringResource(R.string.insights_no_change)
                    category.previous == 0L -> stringResource(R.string.insights_new)
                    else -> (if (difference > 0) "+" else "−") + Money.format(abs(difference))
                }, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                    modifier = Modifier.widthIn(max = 140.dp).background(if (difference == 0L || category.previous == 0L) NeoColors.Concrete else if (difference > 0) NeoColors.Coral else NeoColors.Success)
                        .padding(6.dp))
            }
        }
        if (data.changes.size > 5) Text(stringResource(R.string.insights_biggest_changes), style = NeoTypography.bodySmall)
    }
}
