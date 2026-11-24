package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.alarm.SystemSoundCatalog
import com.example.alarm.SystemSoundOption
import com.example.data.model.AlarmSoundMode
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithAttachments
import com.example.util.AttachmentStorage
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ReminderPreset(@StringRes val labelRes: Int, val minutes: Int)

private val REMINDER_PRESETS =
  listOf(
    ReminderPreset(R.string.preset_on_time, 0),
    ReminderPreset(R.string.preset_10m, 10),
    ReminderPreset(R.string.preset_30m, 30),
    ReminderPreset(R.string.preset_1h, 60),
    ReminderPreset(R.string.preset_1d, 1440),
    ReminderPreset(R.string.preset_3d, 4320),
    ReminderPreset(R.string.preset_1w, 10080),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
  taskId: Long?,
  initialDay: DayKey,
  loadTask: suspend (Long) -> TaskWithAttachments?,
  onSave: (TaskEntity, List<TaskAttachmentEntity>) -> Unit,
  onNavigateBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  BackHandler(onBack = onNavigateBack)

  val defaultCal = remember(initialDay) {
    val now = Calendar.getInstance()
    initialDay.toCalendarAt(hourOfDay = (now.get(Calendar.HOUR_OF_DAY) + 1).coerceAtMost(23), minute = 0)
  }

  var existingTask by remember { mutableStateOf<TaskEntity?>(null) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var dueAtMillis by remember { mutableLongStateOf(defaultCal.timeInMillis) }
  var recurrence by remember { mutableStateOf(RecurrenceType.NONE) }
  var reminderOffsetMinutes by remember { mutableIntStateOf(0) }
  var alarmEnabled by remember { mutableStateOf(true) }
  var alarmSoundMode by remember { mutableStateOf(AlarmSoundMode.SYSTEM) }
  var alarmSoundUri by remember {
    mutableStateOf(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString())
  }
  var alarmSoundTitle by remember {
    mutableStateOf(context.getString(R.string.sound_default_system))
  }
  var vibrate by remember { mutableStateOf(true) }

  val attachments = remember { mutableStateListOf<TaskAttachmentEntity>() }

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showSystemSoundsSheet by remember { mutableStateOf(false) }
  var titleError by remember { mutableStateOf(false) }

  // Preview audio player
  var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
  var isPreviewPlaying by remember { mutableStateOf(false) }

  fun stopPreview() {
    runCatching {
      previewPlayer?.let {
        if (it.isPlaying) it.stop()
        it.release()
      }
    }
    previewPlayer = null
    isPreviewPlaying = false
  }

  fun startPreview() {
    stopPreview()
    if (alarmSoundMode == AlarmSoundMode.SILENT) {
      Toast.makeText(
          context,
          context.getString(R.string.editor_sound_silent_toast),
          Toast.LENGTH_SHORT,
        )
        .show()
      return
    }
    val uri =
      when {
        !alarmSoundUri.isNullOrBlank() && alarmSoundUri!!.startsWith("/") ->
          Uri.fromFile(File(alarmSoundUri!!))
        !alarmSoundUri.isNullOrBlank() -> Uri.parse(alarmSoundUri)
        else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      }
    if (uri == null) {
      Toast.makeText(
          context,
          context.getString(R.string.editor_sound_none_toast),
          Toast.LENGTH_SHORT,
        )
        .show()
      return
    }
    runCatching {
        val player =
          MediaPlayer().apply {
            setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            setDataSource(context, uri)
            setOnCompletionListener { isPreviewPlaying = false }
            prepare()
            start()
          }
        previewPlayer = player
        isPreviewPlaying = true
      }
      .onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.editor_sound_play_error),
            Toast.LENGTH_SHORT,
          )
          .show()
      }
  }

  DisposableEffect(Unit) { onDispose { stopPreview() } }

  LaunchedEffect(taskId) {
    if (taskId != null && taskId > 0L) {
      val loaded = loadTask(taskId)
      if (loaded != null) {
        val t = loaded.task
        existingTask = t
        title = t.title
        description = t.description
        dueAtMillis = t.dueAtMillis
        recurrence = t.recurrence
        reminderOffsetMinutes = t.reminderOffsetMinutes
        alarmEnabled = t.alarmEnabled
        alarmSoundMode = t.alarmSoundMode
        alarmSoundUri = t.alarmSoundUri
        alarmSoundTitle =
          t.alarmSoundTitle.ifBlank {
            if (t.alarmSoundMode == AlarmSoundMode.SILENT) {
              context.getString(R.string.sound_silent)
            } else {
              context.getString(R.string.sound_default_system)
            }
          }
        vibrate = t.vibrate
        attachments.clear()
        attachments.addAll(loaded.attachments)
      }
    }
  }

  val attachFilesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      if (uris.isNotEmpty()) {
        scope.launch {
          val copied =
            withContext(Dispatchers.IO) {
              uris.mapNotNull { uri -> AttachmentStorage.copyUriToAttachments(context, uri) }
            }
          copied.forEach { file ->
            attachments.add(
              TaskAttachmentEntity(
                taskId = existingTask?.id ?: 0L,
                displayName = file.displayName,
                mimeType = file.mimeType,
                localFilePath = file.localFilePath,
                sizeBytes = file.sizeBytes,
              )
            )
          }
          if (copied.isNotEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.editor_attachments_added_toast, copied.size),
                Toast.LENGTH_SHORT,
              )
              .show()
          }
        }
      }
    }

  val manualAudioLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          val copied =
            withContext(Dispatchers.IO) { AttachmentStorage.copyUriToSounds(context, uri) }
          if (copied != null) {
            alarmSoundMode = AlarmSoundMode.MANUAL
            alarmSoundUri = copied.localFilePath
            alarmSoundTitle = copied.displayName
            Toast.makeText(
                context,
                context.getString(R.string.editor_sound_imported_toast, copied.displayName),
                Toast.LENGTH_SHORT,
              )
              .show()
          } else {
            Toast.makeText(
                context,
                context.getString(R.string.editor_sound_import_error),
                Toast.LENGTH_SHORT,
              )
              .show()
          }
        }
      }
    }

  val systemRingtonePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      if (res.resultCode == Activity.RESULT_OK) {
        @Suppress("DEPRECATION")
        val pickedUri =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
          } else {
            res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
          }
        if (pickedUri != null) {
          alarmSoundMode = AlarmSoundMode.SYSTEM
          alarmSoundUri = pickedUri.toString()
          alarmSoundTitle = SystemSoundCatalog.resolveSoundTitle(context, pickedUri)
        } else {
          alarmSoundMode = AlarmSoundMode.SILENT
          alarmSoundUri = null
          alarmSoundTitle = context.getString(R.string.sound_silent)
        }
      }
    }

  fun performSave() {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      titleError = true
      return
    }
    stopPreview()
    val base = existingTask
    val entity =
      if (base != null) {
        base.copy(
          title = cleanTitle,
          description = description.trim(),
          dueAtMillis = dueAtMillis,
          recurrence = recurrence,
          reminderOffsetMinutes = reminderOffsetMinutes.coerceIn(0, 10080),
          alarmEnabled = alarmEnabled,
          alarmSoundMode = alarmSoundMode,
          alarmSoundUri = alarmSoundUri,
          alarmSoundTitle = alarmSoundTitle,
          vibrate = vibrate,
        )
      } else {
        TaskEntity(
          title = cleanTitle,
          description = description.trim(),
          dueAtMillis = dueAtMillis,
          recurrence = recurrence,
          reminderOffsetMinutes = reminderOffsetMinutes.coerceIn(0, 10080),
          alarmEnabled = alarmEnabled,
          alarmSoundMode = alarmSoundMode,
          alarmSoundUri = alarmSoundUri,
          alarmSoundTitle = alarmSoundTitle,
          vibrate = vibrate,
        )
      }
    onSave(entity, attachments.toList())
  }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      performSave()
    }

  fun triggerSaveWithPermissionCheck() {
    if (title.trim().isEmpty()) {
      titleError = true
      return
    }
    if (
      alarmEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      performSave()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(
              if (existingTask == null) R.string.editor_new_title else R.string.editor_edit_title
            )
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("editor_back_button")) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        actions = {
          TextButton(
            onClick = ::triggerSaveWithPermissionCheck,
            modifier = Modifier.testTag("editor_top_save_button"),
          ) {
            Text(stringResource(R.string.action_save))
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
        // Título & Descrição
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            titleError = false
          },
          label = { Text(stringResource(R.string.editor_title_label)) },
          placeholder = { Text(stringResource(R.string.editor_title_placeholder)) },
          isError = titleError,
          supportingText =
            if (titleError) ({ Text(stringResource(R.string.editor_title_error)) }) else null,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(stringResource(R.string.editor_desc_label)) },
          placeholder = { Text(stringResource(R.string.editor_desc_placeholder)) },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("task_description_input"),
        )

        // Data e Horário
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              stringResource(R.string.editor_datetime_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f).testTag("task_date_picker_button"),
              ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(DateTimeUtils.formatShortDate(dueAtMillis))
              }
              OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f).testTag("task_time_picker_button"),
              ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(DateTimeUtils.formatTime(dueAtMillis))
              }
            }
          }
        }

        // Repetição
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                stringResource(R.string.editor_recurrence_title),
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              RecurrenceType.entries.forEach { option ->
                FilterChip(
                  selected = recurrence == option,
                  onClick = { recurrence = option },
                  label = { Text(stringResource(option.labelRes)) },
                  modifier = Modifier.testTag("recurrence_${option.name.lowercase()}"),
                )
              }
            }
          }
        }

        // Prazo para relembrar (até 1 semana antes)
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                stringResource(R.string.editor_reminder_title),
                style = MaterialTheme.typography.titleMedium,
              )
            }

            Text(
              text = DateTimeUtils.formatReminderOffset(context, reminderOffsetMinutes),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              REMINDER_PRESETS.forEach { preset ->
                FilterChip(
                  selected = reminderOffsetMinutes == preset.minutes,
                  onClick = { reminderOffsetMinutes = preset.minutes },
                  label = { Text(stringResource(preset.labelRes)) },
                )
              }
            }

            Slider(
              value = reminderOffsetMinutes.toFloat(),
              onValueChange = { reminderOffsetMinutes = it.toInt().coerceIn(0, 10080) },
              valueRange = 0f..10080f,
              modifier = Modifier.testTag("reminder_offset_slider"),
            )

            if (reminderOffsetMinutes > 0) {
              val reminderAt = dueAtMillis - reminderOffsetMinutes * 60_000L
              Text(
                text =
                  stringResource(
                    R.string.editor_reminder_preview,
                    DateTimeUtils.formatDateTime(reminderAt),
                  ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Alarme e Som
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
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
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  Icons.Rounded.Alarm,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                  Text(
                    stringResource(R.string.editor_alarm_title),
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    stringResource(R.string.editor_alarm_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = alarmEnabled,
                onCheckedChange = { alarmEnabled = it },
                modifier = Modifier.testTag("alarm_enabled_switch"),
              )
            }

            AnimatedVisibility(visible = alarmEnabled) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  AlarmSoundMode.entries.forEach { mode ->
                    FilterChip(
                      selected = alarmSoundMode == mode,
                      onClick = {
                        alarmSoundMode = mode
                        if (mode == AlarmSoundMode.SILENT) {
                          alarmSoundTitle = context.getString(R.string.sound_silent)
                          stopPreview()
                        } else if (mode == AlarmSoundMode.SYSTEM && alarmSoundUri == null) {
                          alarmSoundUri =
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                          alarmSoundTitle = context.getString(R.string.sound_default_system)
                        }
                      },
                      label = { Text(stringResource(mode.labelRes)) },
                      modifier = Modifier.testTag("sound_mode_${mode.name.lowercase()}"),
                    )
                  }
                }

                if (alarmSoundMode == AlarmSoundMode.SYSTEM) {
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = stringResource(R.string.editor_sound_selected, alarmSoundTitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                          onClick = { showSystemSoundsSheet = true },
                          modifier = Modifier.weight(1f).testTag("pick_system_sound_list"),
                        ) {
                          Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                          Spacer(Modifier.width(6.dp))
                          Text(stringResource(R.string.editor_sound_device_list))
                        }
                        OutlinedButton(
                          onClick = {
                            val pickerIntent =
                              Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                  RingtoneManager.EXTRA_RINGTONE_TYPE,
                                  RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION,
                                )
                                putExtra(
                                  RingtoneManager.EXTRA_RINGTONE_TITLE,
                                  context.getString(R.string.editor_sound_picker_title),
                                )
                                alarmSoundUri?.let {
                                  runCatching { Uri.parse(it) }.getOrNull()?.let { uri ->
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri)
                                  }
                                }
                              }
                            systemRingtonePickerLauncher.launch(pickerIntent)
                          },
                          modifier = Modifier.weight(1f),
                        ) {
                          Text(stringResource(R.string.editor_sound_android_picker))
                        }
                      }
                    }
                  }
                } else if (alarmSoundMode == AlarmSoundMode.MANUAL) {
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text =
                          if (alarmSoundUri != null) {
                            stringResource(R.string.editor_sound_manual_file, alarmSoundTitle)
                          } else {
                            stringResource(R.string.editor_sound_manual_none)
                          },
                        style = MaterialTheme.typography.bodyMedium,
                      )
                      FilledTonalButton(
                        onClick = { manualAudioLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("pick_manual_audio_button"),
                      ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.editor_sound_manual_pick))
                      }
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      stringResource(R.string.editor_vibrate_label),
                      style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                  }

                  if (alarmSoundMode != AlarmSoundMode.SILENT) {
                    OutlinedButton(
                      onClick = { if (isPreviewPlaying) stopPreview() else startPreview() },
                      modifier = Modifier.testTag("test_alarm_sound_button"),
                    ) {
                      Icon(
                        imageVector =
                          if (isPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(Modifier.width(4.dp))
                      Text(
                        stringResource(
                          if (isPreviewPlaying) R.string.editor_sound_stop
                          else R.string.editor_sound_test
                        )
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Arquivos Anexos
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Rounded.AttachFile,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  stringResource(R.string.editor_attachments_title),
                  style = MaterialTheme.typography.titleMedium,
                )
              }
              FilledTonalButton(
                onClick = { attachFilesLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.testTag("attach_files_button"),
              ) {
                Text(stringResource(R.string.editor_attach_button))
              }
            }

            if (attachments.isEmpty()) {
              Text(
                text = stringResource(R.string.editor_attachments_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              attachments.forEachIndexed { idx, att ->
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceContainer,
                  modifier =
                    Modifier.fillMaxWidth().clickable {
                      AttachmentStorage.openAttachment(context, att)
                    },
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    if (att.mimeType.startsWith("image/")) {
                      AsyncImage(
                        model = File(att.localFilePath),
                        contentDescription = att.displayName,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                      )
                    } else {
                      Box(
                        modifier =
                          Modifier.size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                      ) {
                        Icon(
                          Icons.Rounded.AttachFile,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                      }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = att.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = AttachmentStorage.formatSize(att.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    IconButton(onClick = { attachments.removeAt(idx) }) {
                      Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.editor_remove_attachment),
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Button(
          onClick = ::triggerSaveWithPermissionCheck,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_task_button"),
        ) {
          Icon(Icons.Rounded.Save, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            stringResource(
              if (existingTask == null) R.string.editor_save_task else R.string.editor_update_task
            )
          )
        }

        Spacer(Modifier.height(24.dp))
      }
    }
  }

  if (showDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.localMillisToUtcPickerMidnight(dueAtMillis)
      )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { pickedUtc ->
              val cur = Calendar.getInstance().apply { timeInMillis = dueAtMillis }
              dueAtMillis =
                DateTimeUtils.mergeDateAndTime(
                  pickedUtc,
                  cur.get(Calendar.HOUR_OF_DAY),
                  cur.get(Calendar.MINUTE),
                )
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val cur = remember(dueAtMillis) { Calendar.getInstance().apply { timeInMillis = dueAtMillis } }
    val timePickerState =
      rememberTimePickerState(
        initialHour = cur.get(Calendar.HOUR_OF_DAY),
        initialMinute = cur.get(Calendar.MINUTE),
        is24Hour = DateFormat.is24HourFormat(context),
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text(stringResource(R.string.editor_pick_time_title)) },
      text = { TimePicker(state = timePickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            val updated =
              Calendar.getInstance().apply {
                timeInMillis = dueAtMillis
                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                set(Calendar.MINUTE, timePickerState.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
            dueAtMillis = updated.timeInMillis
            showTimePicker = false
          }
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showSystemSoundsSheet) {
    val systemSounds = remember { SystemSoundCatalog.loadSystemSounds(context) }
    ModalBottomSheet(onDismissRequest = { showSystemSoundsSheet = false }) {
      Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
          text = stringResource(R.string.editor_system_sounds_title),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
          items(items = systemSounds, key = { it.uriString }) { option: SystemSoundOption ->
            val selected = alarmSoundUri == option.uriString
            ListItem(
              headlineContent = { Text(option.title) },
              supportingContent = { Text(option.category) },
              leadingContent = {
                RadioButton(
                  selected = selected,
                  onClick = {
                    alarmSoundMode = AlarmSoundMode.SYSTEM
                    alarmSoundUri = option.uriString
                    alarmSoundTitle = option.title
                    showSystemSoundsSheet = false
                  },
                )
              },
              modifier =
                Modifier.clickable {
                  alarmSoundMode = AlarmSoundMode.SYSTEM
                  alarmSoundUri = option.uriString
                  alarmSoundTitle = option.title
                  showSystemSoundsSheet = false
                },
            )
          }
        }
      }
    }
  }
}
