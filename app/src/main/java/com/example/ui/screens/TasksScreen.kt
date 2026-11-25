package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ScheduleSend
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AlarmSoundMode
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.CalendarPanel
import com.example.ui.viewmodel.CalendarTaskItem
import com.example.ui.viewmodel.TaskFilter
import com.example.ui.viewmodel.TasksUiState
import com.example.util.AttachmentStorage
import com.example.util.DateTimeUtils
import com.example.util.DayKey

@Composable
fun TasksScreen(
  state: TasksUiState,
  onSelectDay: (DayKey) -> Unit,
  onChangeMonth: (Int) -> Unit,
  onToggleCalendar: () -> Unit,
  onJumpToday: () -> Unit,
  onFilterChange: (TaskFilter) -> Unit,
  onCompleteTask: (TaskEntity) -> Unit,
  onRescheduleTask: (Long) -> Unit,
  onEditTask: (Long) -> Unit,
  onDeleteTask: (TaskEntity) -> Unit,
  onCreateTaskForDay: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var taskPendingDelete by remember { mutableStateOf<TaskEntity?>(null) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "calendar_panel") {
        CalendarPanel(
          selectedDay = state.selectedDay,
          displayedYear = state.displayedYear,
          displayedMonth = state.displayedMonth,
          isExpanded = state.isMonthExpanded,
          taskCountByDay = state.taskCountByDay,
          onSelectDay = onSelectDay,
          onChangeMonth = onChangeMonth,
          onToggleExpanded = onToggleCalendar,
          onJumpToday = onJumpToday,
        )
      }

      item(key = "filter_row") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TaskFilter.entries.forEach { filter ->
              FilterChip(
                selected = state.filter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(filter.labelRes)) },
                modifier = Modifier.testTag("task_filter_${filter.name.lowercase()}"),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val headerTitle =
              when (state.filter) {
                TaskFilter.SELECTED_DAY -> DateTimeUtils.formatDayHeader(state.selectedDay)
                TaskFilter.ALL_PENDING -> stringResource(R.string.tasks_all_pending_header)
                TaskFilter.COMPLETED -> stringResource(R.string.tasks_completed_header)
              }
            Text(
              text = headerTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = stringResource(R.string.tasks_count_badge, state.visibleTasks.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      if (state.visibleTasks.isEmpty()) {
        item(key = "empty_tasks") {
          EmptyTasksCard(filter = state.filter, onCreateTask = onCreateTaskForDay)
        }
      } else {
        items(
          items = state.visibleTasks,
          key = { "${it.taskWithAttachments.task.id}_${it.displayDueAtMillis}" },
        ) { item ->
          TaskCard(
            item = item,
            showDateInBadge = state.filter != TaskFilter.SELECTED_DAY,
            onComplete = { onCompleteTask(item.taskWithAttachments.task) },
            onReschedule = { onRescheduleTask(item.taskWithAttachments.task.id) },
            onEdit = { onEditTask(item.taskWithAttachments.task.id) },
            onDelete = { taskPendingDelete = item.taskWithAttachments.task },
          )
        }
      }
    }
  }

  taskPendingDelete?.let { task ->
    AlertDialog(
      onDismissRequest = { taskPendingDelete = null },
      title = { Text(stringResource(R.string.task_delete_title)) },
      text = { Text(stringResource(R.string.task_delete_message, task.title)) },
      confirmButton = {
        Button(
          onClick = {
            onDeleteTask(task)
            taskPendingDelete = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { taskPendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun EmptyTasksCard(filter: TaskFilter, onCreateTask: () -> Unit) {
  ElevatedCard(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_tasks),
        contentDescription = stringResource(R.string.tasks_empty_cd),
        modifier = Modifier.fillMaxWidth(0.7f).height(150.dp).clip(RoundedCornerShape(18.dp)),
        contentScale = ContentScale.Crop,
      )
      Text(
        text =
          when (filter) {
            TaskFilter.SELECTED_DAY -> stringResource(R.string.tasks_empty_day)
            TaskFilter.ALL_PENDING -> stringResource(R.string.tasks_empty_pending)
            TaskFilter.COMPLETED -> stringResource(R.string.tasks_empty_completed)
          },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.tasks_empty_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (filter != TaskFilter.COMPLETED) {
        Button(onClick = onCreateTask, modifier = Modifier.testTag("empty_create_task_button")) {
          Text(stringResource(R.string.tasks_empty_cta))
        }
      }
    }
  }
}

@Composable
private fun TaskCard(
  item: CalendarTaskItem,
  showDateInBadge: Boolean,
  onComplete: () -> Unit,
  onReschedule: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val taskWithAttachments = item.taskWithAttachments
  val task = taskWithAttachments.task
  val displayMillis = item.displayDueAtMillis
  val context = LocalContext.current
  val isOverdue =
    !item.isFutureProjection && !task.isCompleted && displayMillis < System.currentTimeMillis()

  ElevatedCard(
    modifier =
      Modifier.fillMaxWidth()
        .alpha(if (task.isCompleted) 0.72f else 1f)
        .clickable(onClick = onEdit)
        .testTag("task_card_${task.id}"),
    shape = RoundedCornerShape(22.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor =
          if (item.isFutureProjection) MaterialTheme.colorScheme.surfaceContainerLowest
          else MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                when {
                  task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                  isOverdue -> MaterialTheme.colorScheme.errorContainer
                  item.isFutureProjection -> MaterialTheme.colorScheme.tertiaryContainer
                  else -> MaterialTheme.colorScheme.primaryContainer
                },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Schedule,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint =
                    when {
                      task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                      isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                      item.isFutureProjection -> MaterialTheme.colorScheme.onTertiaryContainer
                      else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
                Text(
                  text =
                    if (showDateInBadge) DateTimeUtils.formatDateTime(displayMillis)
                    else DateTimeUtils.formatTime(displayMillis),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color =
                    when {
                      task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                      isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                      item.isFutureProjection -> MaterialTheme.colorScheme.onTertiaryContainer
                      else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
              }
            }

            if (item.isFutureProjection) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.EventRepeat,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Text(
                    text = stringResource(R.string.task_upcoming_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            }

            if (task.alarmEnabled && !task.isCompleted) {
              Icon(
                imageVector =
                  if (task.alarmSoundMode == AlarmSoundMode.SILENT) Icons.Rounded.AlarmOff
                  else Icons.Rounded.Alarm,
                contentDescription = stringResource(R.string.task_alarm_active_cd),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            textDecoration =
              if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = MaterialTheme.colorScheme.onSurface,
          )

          if (task.description.isNotBlank()) {
            Text(
              text = task.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_task_${task.id}")) {
            Icon(
              Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.action_edit),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_task_${task.id}")) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.action_delete),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (task.recurrence != RecurrenceType.NONE) {
          InfoPill(icon = Icons.Rounded.Repeat, text = stringResource(task.recurrence.labelRes))
        }
        if (task.reminderOffsetMinutes > 0) {
          InfoPill(
            icon = Icons.Rounded.NotificationsActive,
            text = DateTimeUtils.formatReminderOffset(context, task.reminderOffsetMinutes),
          )
        }
        if (task.alarmEnabled) {
          val resolvedSoundTitle =
            task.alarmSoundTitle.ifBlank { stringResource(R.string.sound_default_system) }
          InfoPill(icon = Icons.Rounded.Alarm, text = resolvedSoundTitle)
        }
      }

      AnimatedVisibility(visible = taskWithAttachments.attachments.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          taskWithAttachments.attachments.forEach { att ->
            AttachmentChip(
              attachment = att,
              onClick = {
                val opened = AttachmentStorage.openAttachment(context, att)
                if (!opened) {
                  Toast.makeText(
                      context,
                      context.getString(R.string.task_open_attachment_error, att.displayName),
                      Toast.LENGTH_SHORT,
                    )
                    .show()
                }
              },
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onComplete,
          modifier = Modifier.weight(1f).testTag("complete_task_${task.id}"),
          colors =
            if (task.isCompleted) {
              ButtonDefaults.filledTonalButtonColors()
            } else {
              ButtonDefaults.buttonColors()
            },
        ) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text =
              when {
                task.isCompleted -> stringResource(R.string.task_reopen)
                task.recurrence != RecurrenceType.NONE -> stringResource(R.string.task_complete_cycle)
                else -> stringResource(R.string.task_complete)
              }
          )
        }

        OutlinedButton(
          onClick = onReschedule,
          modifier = Modifier.weight(1f).testTag("reschedule_task_${task.id}"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ScheduleSend,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.task_postpone))
        }
      }
    }
  }
}

@Composable
private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AttachmentChip(attachment: TaskAttachmentEntity, onClick: () -> Unit) {
  AssistChip(
    onClick = onClick,
    label = {
      Text(
        text = "${attachment.displayName} (${AttachmentStorage.formatSize(attachment.sizeBytes)})",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 180.dp),
      )
    },
    leadingIcon = {
      Box(
        modifier =
          Modifier.size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.AttachFile,
          contentDescription = null,
          modifier = Modifier.size(13.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    },
  )
}
