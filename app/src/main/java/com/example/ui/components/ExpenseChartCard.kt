package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.CategorySpendSlice
import com.example.util.DateTimeUtils
import com.example.util.ExpenseCalculator
import java.util.Calendar
import kotlin.math.roundToInt

private enum class ExpenseChartMode {
  BY_CATEGORY,
  DAILY,
  BY_TYPE,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseChartCard(
  displayedYear: Int,
  displayedMonth: Int,
  monthTotalCents: Long,
  monthDebitOneTimeCents: Long,
  monthCreditOneTimeCents: Long,
  monthInstallmentCents: Long,
  monthRecurringCents: Long,
  categoryBreakdown: List<CategorySpendSlice>,
  dailyTotalsInMonth: Map<Int, Long>,
  modifier: Modifier = Modifier,
) {
  var chartMode by remember { mutableStateOf(ExpenseChartMode.BY_CATEGORY) }
  val monthLabel = DateTimeUtils.formatMonthYear(displayedYear, displayedMonth)

  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.expense_chart_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.expense_chart_subtitle, monthLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = ExpenseCalculator.formatCurrency(monthTotalCents),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("expense_month_total_text"),
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        SummaryBadge(
          text =
            stringResource(
              R.string.expense_chart_debit_summary,
              ExpenseCalculator.formatCurrency(monthDebitOneTimeCents),
            ),
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.testTag("expense_summary_debit"),
        )
        SummaryBadge(
          text =
            stringResource(
              R.string.expense_chart_credit_single_summary,
              ExpenseCalculator.formatCurrency(monthCreditOneTimeCents),
            ),
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.testTag("expense_summary_credit_single"),
        )
        SummaryBadge(
          text =
            stringResource(
              R.string.expense_chart_installment_summary,
              ExpenseCalculator.formatCurrency(monthInstallmentCents),
            ),
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.testTag("expense_summary_credit_installment"),
        )
        SummaryBadge(
          text =
            stringResource(
              R.string.expense_chart_recurring_summary,
              ExpenseCalculator.formatCurrency(monthRecurringCents),
            ),
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("expense_summary_recurring"),
        )
      }

      if (monthTotalCents <= 0L) {
        Text(
          text = stringResource(R.string.expense_chart_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 8.dp),
        )
      } else {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = chartMode == ExpenseChartMode.BY_CATEGORY,
            onClick = { chartMode = ExpenseChartMode.BY_CATEGORY },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.PieChart,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(stringResource(R.string.expense_chart_by_category)) },
            modifier = Modifier.testTag("chart_mode_category"),
          )
          FilterChip(
            selected = chartMode == ExpenseChartMode.DAILY,
            onClick = { chartMode = ExpenseChartMode.DAILY },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(stringResource(R.string.expense_chart_by_day)) },
            modifier = Modifier.testTag("chart_mode_day"),
          )
          FilterChip(
            selected = chartMode == ExpenseChartMode.BY_TYPE,
            onClick = { chartMode = ExpenseChartMode.BY_TYPE },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(stringResource(R.string.expense_chart_by_type)) },
            modifier = Modifier.testTag("chart_mode_type"),
          )
        }

        when (chartMode) {
          ExpenseChartMode.BY_CATEGORY -> CategoryDonutChart(slices = categoryBreakdown)
          ExpenseChartMode.DAILY ->
            DailyBarChart(
              displayedYear = displayedYear,
              displayedMonth = displayedMonth,
              dailyTotalsInMonth = dailyTotalsInMonth,
            )
          ExpenseChartMode.BY_TYPE ->
            PaymentTypeBreakdownChart(
              monthTotalCents = monthTotalCents,
              debitOneTimeCents = monthDebitOneTimeCents,
              creditOneTimeCents = monthCreditOneTimeCents,
              creditInstallmentCents = monthInstallmentCents,
              recurringCents = monthRecurringCents,
            )
        }
      }
    }
  }
}

@Composable
private fun SummaryBadge(
  text: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(shape = RoundedCornerShape(999.dp), color = containerColor, modifier = modifier) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = contentColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun PaymentTypeBreakdownChart(
  monthTotalCents: Long,
  debitOneTimeCents: Long,
  creditOneTimeCents: Long,
  creditInstallmentCents: Long,
  recurringCents: Long,
) {
  val rows =
    listOf(
      Triple(
        stringResource(R.string.expense_type_debit_one_time),
        debitOneTimeCents,
        MaterialTheme.colorScheme.secondary,
      ),
      Triple(
        stringResource(R.string.expense_type_credit_one_time),
        creditOneTimeCents,
        MaterialTheme.colorScheme.primary,
      ),
      Triple(
        stringResource(R.string.expense_type_credit_installment),
        creditInstallmentCents,
        MaterialTheme.colorScheme.tertiary,
      ),
      Triple(
        stringResource(R.string.expense_type_recurring),
        recurringCents,
        MaterialTheme.colorScheme.outline,
      ),
    )

  Column(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    rows.forEach { (label, amountCents, barColor) ->
      val fraction =
        if (monthTotalCents > 0L) (amountCents.toFloat() / monthTotalCents.toFloat()).coerceIn(0f, 1f)
        else 0f
      val percent = (fraction * 100f).roundToInt().coerceIn(0, 100)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${ExpenseCalculator.formatCurrency(amountCents)} ($percent%)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        LinearProgressIndicator(
          progress = { fraction },
          color = barColor,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
        )
      }
    }
  }
}

@Composable
private fun CategoryDonutChart(slices: List<CategorySpendSlice>) {
  val uncategorizedColor = MaterialTheme.colorScheme.outline
  val centerHoleColor = MaterialTheme.colorScheme.surfaceContainerLow

  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Canvas(modifier = Modifier.size(128.dp).testTag("expense_donut_chart")) {
      val diameter = size.minDimension
      val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
      val arcSize = Size(diameter, diameter)
      val gapDegrees = if (slices.size > 1) 2.5f else 0f
      var startAngle = -90f

      slices.forEach { slice ->
        val sweep = (slice.fraction * 360f) - gapDegrees
        val sliceColor =
          slice.category?.let { Color(it.colorArgb.toInt()) } ?: uncategorizedColor
        if (sweep > 0.5f) {
          drawArc(
            color = sliceColor,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
          )
        }
        startAngle += slice.fraction * 360f
      }

      drawCircle(
        color = centerHoleColor,
        radius = diameter * 0.29f,
        center = Offset(size.width / 2f, size.height / 2f),
      )
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      slices.forEach { slice ->
        val sliceColor =
          slice.category?.let { Color(it.colorArgb.toInt()) } ?: uncategorizedColor
        val label =
          slice.category?.name ?: stringResource(R.string.expense_category_uncategorized)
        val percent = (slice.fraction * 100f).roundToInt().coerceIn(0, 100)

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(sliceColor))
          Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "${ExpenseCalculator.formatCurrency(slice.totalCents)} ($percent%)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun DailyBarChart(
  displayedYear: Int,
  displayedMonth: Int,
  dailyTotalsInMonth: Map<Int, Long>,
) {
  val daysInMonth =
    remember(displayedYear, displayedMonth) {
      Calendar.getInstance()
        .apply {
          set(Calendar.YEAR, displayedYear)
          set(Calendar.MONTH, displayedMonth)
          set(Calendar.DAY_OF_MONTH, 1)
        }
        .getActualMaximum(Calendar.DAY_OF_MONTH)
    }

  val peakEntry = remember(dailyTotalsInMonth) { dailyTotalsInMonth.maxByOrNull { it.value } }
  val maxDailyCents = (peakEntry?.value ?: 1L).coerceAtLeast(1L)

  val barColor = MaterialTheme.colorScheme.primary
  val peakBarColor = MaterialTheme.colorScheme.tertiary
  val emptyTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
  val axisColor = MaterialTheme.colorScheme.outlineVariant

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Canvas(modifier = Modifier.fillMaxWidth().height(132.dp).testTag("expense_daily_bar_chart")) {
      val slotWidth = size.width / daysInMonth.toFloat()
      val barWidth = (slotWidth * 0.62f).coerceAtLeast(2f)
      val barOffsetX = (slotWidth - barWidth) / 2f
      val chartBottom = size.height - 4.dp.toPx()
      val usableHeight = (chartBottom - 6.dp.toPx()).coerceAtLeast(8f)

      drawLine(
        color = axisColor,
        start = Offset(0f, chartBottom),
        end = Offset(size.width, chartBottom),
        strokeWidth = 1.dp.toPx(),
      )

      for (day in 1..daysInMonth) {
        val x = (day - 1) * slotWidth + barOffsetX
        val amount = dailyTotalsInMonth[day] ?: 0L
        if (amount > 0L) {
          val height = ((amount.toFloat() / maxDailyCents.toFloat()) * usableHeight).coerceAtLeast(6f)
          val color = if (peakEntry?.key == day) peakBarColor else barColor
          drawRoundRect(
            color = color,
            topLeft = Offset(x, chartBottom - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
          )
        } else {
          val stubHeight = 3.dp.toPx()
          drawRoundRect(
            color = emptyTrackColor,
            topLeft = Offset(x, chartBottom - stubHeight),
            size = Size(barWidth, stubHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
          )
        }
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      listOf("1", "10", "20", daysInMonth.toString()).forEach { mark ->
        Text(
          text = mark,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    peakEntry?.let { (day, cents) ->
      if (cents > 0L) {
        Text(
          text =
            stringResource(
              R.string.expense_chart_peak_day,
              day,
              ExpenseCalculator.formatCurrency(cents),
            ),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.tertiary,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}
