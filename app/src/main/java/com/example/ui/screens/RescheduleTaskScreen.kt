package com.example.ui.screens

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ScheduleSend
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithAttachments
import com.example.util.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleTaskScreen(
  taskId: Long,
  loadTask: suspend (Long) -> TaskWithAttachments?,
  onConfirmReschedule: (Long, Long) -> Unit,
  onNavigateBack: () -> Unit,
) {
  val context = LocalContext.current
  BackHandler(onBack = onNavigateBack)

  var task by remember { mutableStateOf<TaskEntity?>(null) }
  var selectedMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 3600_000L) }

  LaunchedEffect(taskId) {
    val loaded = loadTask(taskId)
    if (loaded != null) {
      task = loaded.task
      selectedMillis = maxOf(loaded.task.dueAtMillis, System.currentTimeMillis())
    }
  }

  val initialCal =
    remember(task) { Calendar.getInstance().apply { timeInMillis = selectedMillis } }

  val datePickerState =
    rememberDatePickerState(
      initialSelectedDateMillis = DateTimeUtils.localMillisToUtcPickerMidnight(selectedMillis)
    )
  val timePickerState =
    rememberTimePickerState(
      initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
      initialMinute = initialCal.get(Calendar.MINUTE),
      is24Hour = DateFormat.is24HourFormat(context),
    )

  // Keep picker states synced when task loads
  LaunchedEffect(task?.id) {
    task?.let { loadedTask ->
      val baseMillis = maxOf(loadedTask.dueAtMillis, System.currentTimeMillis())
      datePickerState.selectedDateMillis = DateTimeUtils.localMillisToUtcPickerMidnight(baseMillis)
    }
  }

  fun applyQuickOffset(addMillis: Long) {
    val base = System.currentTimeMillis() + addMillis
    selectedMillis = base
    datePickerState.selectedDateMillis = DateTimeUtils.localMillisToUtcPickerMidnight(base)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.reschedule_title)) },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("reschedule_back_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        task?.let { t ->
          ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors =
              CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              ),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.ScheduleSend,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text(text = t.title, style = MaterialTheme.typography.titleMedium)
                Text(
                  text =
                    stringResource(
                      R.string.reschedule_previous,
                      DateTimeUtils.formatDateTime(t.dueAtMillis),
                    ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Quick postpone shortcuts
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AssistChip(
            onClick = { applyQuickOffset(3600_000L) },
            label = { Text(stringResource(R.string.reschedule_plus_1h)) },
            modifier = Modifier.testTag("postpone_1h_chip"),
          )
          AssistChip(
            onClick = { applyQuickOffset(24 * 3600_000L) },
            label = { Text(stringResource(R.string.reschedule_tomorrow)) },
            modifier = Modifier.testTag("postpone_tomorrow_chip"),
          )
          AssistChip(
            onClick = { applyQuickOffset(3 * 24 * 3600_000L) },
            label = { Text(stringResource(R.string.reschedule_plus_3d)) },
          )
          AssistChip(
            onClick = { applyQuickOffset(7 * 24 * 3600_000L) },
            label = { Text(stringResource(R.string.reschedule_plus_1w)) },
          )
        }

        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
              text = stringResource(R.string.reschedule_pick_date),
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            DatePicker(state = datePickerState, title = null, headline = null, showModeToggle = true)
          }
        }

        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.reschedule_pick_time),
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.align(Alignment.Start),
            )
            TimePicker(state = timePickerState)
          }
        }

        Button(
          onClick = {
            val dateUtc =
              datePickerState.selectedDateMillis
                ?: DateTimeUtils.localMillisToUtcPickerMidnight(selectedMillis)
            val newMillis =
              DateTimeUtils.mergeDateAndTime(dateUtc, timePickerState.hour, timePickerState.minute)
            onConfirmReschedule(taskId, newMillis)
          },
          modifier =
            Modifier.fillMaxWidth().height(54.dp).testTag("confirm_reschedule_button"),
        ) {
          Icon(Icons.Rounded.Check, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.reschedule_confirm))
        }

        Spacer(Modifier.height(24.dp))
      }
    }
  }
}
