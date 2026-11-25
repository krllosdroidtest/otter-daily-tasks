package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import java.util.Locale

@Composable
fun CalendarPanel(
  selectedDay: DayKey,
  displayedYear: Int,
  displayedMonth: Int,
  isExpanded: Boolean,
  taskCountByDay: Map<DayKey, Int>,
  onSelectDay: (DayKey) -> Unit,
  onChangeMonth: (Int) -> Unit,
  onToggleExpanded: () -> Unit,
  onJumpToday: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val today = DayKey.today()
  val locale = Locale.getDefault()
  val weekdayLabels = remember(locale) { DateTimeUtils.weekdayInitials() }

  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = DateTimeUtils.formatMonthYear(displayedYear, displayedMonth),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (selectedDay != today) {
            AssistChip(
              onClick = onJumpToday,
              label = {
                Text(stringResource(R.string.today), style = MaterialTheme.typography.labelMedium)
              },
              leadingIcon = {
                Icon(
                  Icons.Rounded.Today,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              modifier = Modifier.testTag("calendar_today_button"),
            )
          }
          IconButton(
            onClick = { onChangeMonth(-1) },
            modifier = Modifier.testTag("calendar_prev_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = stringResource(R.string.calendar_prev_month),
            )
          }
          IconButton(
            onClick = { onChangeMonth(1) },
            modifier = Modifier.testTag("calendar_next_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = stringResource(R.string.calendar_next_month),
            )
          }
          IconButton(
            onClick = onToggleExpanded,
            modifier = Modifier.testTag("calendar_toggle_expand"),
          ) {
            Icon(
              imageVector =
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              contentDescription =
                stringResource(
                  if (isExpanded) R.string.calendar_collapse else R.string.calendar_expand
                ),
            )
          }
        }
      }

      Spacer(Modifier.height(6.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        weekdayLabels.forEach { label ->
          Text(
            text = label,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(Modifier.height(4.dp))

      AnimatedContent(targetState = isExpanded, label = "calendar_mode") { expanded ->
        if (expanded) {
          val cells = DateTimeUtils.buildMonthGrid(displayedYear, displayedMonth)
          val rows = cells.chunked(7)
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { week ->
              Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                  Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (day != null) {
                      CalendarDayCell(
                        day = day,
                        isSelected = day == selectedDay,
                        isToday = day == today,
                        taskCount = taskCountByDay[day] ?: 0,
                        onClick = { onSelectDay(day) },
                      )
                    } else {
                      Spacer(Modifier.size(44.dp))
                    }
                  }
                }
              }
            }
          }
        } else {
          val week = DateTimeUtils.buildWeekAround(selectedDay)
          Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { day ->
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CalendarDayCell(
                  day = day,
                  isSelected = day == selectedDay,
                  isToday = day == today,
                  taskCount = taskCountByDay[day] ?: 0,
                  onClick = { onSelectDay(day) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarDayCell(
  day: DayKey,
  isSelected: Boolean,
  isToday: Boolean,
  taskCount: Int,
  onClick: () -> Unit,
) {
  val bgColor =
    when {
      isSelected -> MaterialTheme.colorScheme.primary
      isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      else -> androidx.compose.ui.graphics.Color.Transparent
    }
  val textColor =
    when {
      isSelected -> MaterialTheme.colorScheme.onPrimary
      isToday -> MaterialTheme.colorScheme.primary
      else -> MaterialTheme.colorScheme.onSurface
    }
  val borderModifier =
    if (isToday && !isSelected) {
      Modifier.border(
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        RoundedCornerShape(14.dp),
      )
    } else {
      Modifier
    }

  Column(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(14.dp))
        .background(bgColor)
        .then(borderModifier)
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = day.dayOfMonth.toString(),
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
      color = textColor,
    )
    Spacer(Modifier.height(2.dp))
    if (taskCount > 0) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val dots = minOf(taskCount, 3)
        repeat(dots) {
          Box(
            modifier =
              Modifier.size(5.dp)
                .clip(CircleShape)
                .background(
                  if (isSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.secondary
                )
          )
        }
        if (taskCount > 3) {
          Text(
            text = "+",
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isSelected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.secondary,
          )
        }
      }
    } else {
      Spacer(Modifier.height(5.dp))
    }
  }
}
