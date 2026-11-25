package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.backup.BackupPayload
import com.example.backup.BackupPreview
import com.example.data.repository.ThemePreferences
import com.example.ui.theme.AppPalette
import com.example.ui.theme.ThemeMode
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  themePreferences: ThemePreferences,
  backupPreview: BackupPreview?,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetDynamicColor: (Boolean) -> Unit,
  onSetPalette: (AppPalette) -> Unit,
  onExportBackup: (Uri) -> Unit,
  onLoadBackupPreview: (Uri) -> Unit,
  onDismissBackupPreview: () -> Unit,
  onConfirmImport: (BackupPayload, Boolean) -> Unit,
  onNavigateBack: () -> Unit,
) {
  val context = LocalContext.current
  BackHandler(onBack = onNavigateBack)

  val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  var notificationsGranted by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }
  val exactAlarmsAllowed = remember { AlarmScheduler.canScheduleExactAlarms(context) }

  val notifPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      notificationsGranted = granted
    }

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
      uri ->
      if (uri != null) {
        onExportBackup(uri)
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        onLoadBackupPreview(uri)
      }
    }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.nav_settings)) },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("settings_back_button"),
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
        // Section 1: Android Device & Color Personalization
        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(8.dp))
              Text(
                stringResource(R.string.settings_colors_title),
                style = MaterialTheme.typography.titleMedium,
              )
            }

            // Installed Android OS info banner
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  Icons.Rounded.Android,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text(
                    text =
                      stringResource(
                        R.string.settings_android_version,
                        Build.VERSION.RELEASE,
                        Build.VERSION.SDK_INT,
                      ),
                    style = MaterialTheme.typography.labelLarge,
                  )
                  Text(
                    text =
                      stringResource(
                        if (supportsDynamicColor) R.string.settings_dynamic_supported
                        else R.string.settings_dynamic_unsupported
                      ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            // Dynamic Color toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  stringResource(R.string.settings_dynamic_toggle),
                  style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                  text =
                    stringResource(
                      if (supportsDynamicColor) R.string.settings_dynamic_toggle_on
                      else R.string.settings_dynamic_toggle_off
                    ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = themePreferences.useDynamicColor && supportsDynamicColor,
                onCheckedChange = onSetDynamicColor,
                enabled = supportsDynamicColor,
                modifier = Modifier.testTag("dynamic_color_switch"),
              )
            }

            // Live scheme preview swatches
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Rounded.ColorLens,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                stringResource(R.string.settings_active_palette),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              listOf(
                  MaterialTheme.colorScheme.primary,
                  MaterialTheme.colorScheme.secondary,
                  MaterialTheme.colorScheme.tertiary,
                  MaterialTheme.colorScheme.primaryContainer,
                )
                .forEach { swatch ->
                  Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(swatch))
                }
            }

            // Theme mode chips
            Text(
              stringResource(R.string.settings_theme_mode),
              style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ThemeMode.entries.forEach { mode ->
                FilterChip(
                  selected = themePreferences.themeMode == mode,
                  onClick = { onSetThemeMode(mode) },
                  label = { Text(stringResource(mode.labelRes)) },
                  modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
                )
              }
            }

            // Curated palettes (used when dynamic color is off or on Android < 12)
            Text(
              text =
                stringResource(
                  if (themePreferences.useDynamicColor && supportsDynamicColor) {
                    R.string.settings_palette_alt
                  } else {
                    R.string.settings_palette_choose
                  }
                ),
              style = MaterialTheme.typography.labelLarge,
            )

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              AppPalette.entries.forEach { palette ->
                val isSelected =
                  themePreferences.palette == palette &&
                    !(themePreferences.useDynamicColor && supportsDynamicColor)
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer,
                  border =
                    if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                  modifier =
                    Modifier.clickable {
                        if (themePreferences.useDynamicColor && supportsDynamicColor) {
                          onSetDynamicColor(false)
                        }
                        onSetPalette(palette)
                      }
                      .testTag("palette_${palette.name.lowercase()}"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(24.dp).clip(CircleShape).background(palette.previewPrimary),
                      contentAlignment = Alignment.Center,
                    ) {
                      if (isSelected) {
                        Icon(
                          Icons.Rounded.Check,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.size(14.dp),
                        )
                      }
                    }
                    Box(
                      modifier =
                        Modifier.size(14.dp).clip(CircleShape).background(palette.previewSecondary)
                    )
                    Text(
                      stringResource(palette.labelRes),
                      style = MaterialTheme.typography.labelMedium,
                    )
                  }
                }
              }
            }
          }
        }

        // Section 2: Notifications & Exact Alarms
        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(8.dp))
              Text(
                stringResource(R.string.settings_alarm_perms_title),
                style = MaterialTheme.typography.titleMedium,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  stringResource(R.string.settings_notifications),
                  style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                  text =
                    stringResource(
                      if (notificationsGranted) R.string.settings_notifications_on
                      else R.string.settings_notifications_off
                    ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FilledTonalButton(
                  onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                ) {
                  Text(stringResource(R.string.settings_allow))
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  stringResource(R.string.settings_exact_alarms),
                  style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                  text =
                    stringResource(
                      if (exactAlarmsAllowed) R.string.settings_exact_alarms_on
                      else R.string.settings_exact_alarms_off
                    ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (!exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OutlinedButton(
                  onClick = {
                    val intent =
                      Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                      }
                    context.startActivity(intent)
                  }
                ) {
                  Text(stringResource(R.string.settings_configure))
                }
              }
            }
          }
        }

        // Section 3: Offline Export / Import
        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(8.dp))
              Text(
                stringResource(R.string.settings_backup_title),
                style = MaterialTheme.typography.titleMedium,
              )
            }

            Text(
              text = stringResource(R.string.settings_backup_desc),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = {
                  val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                  exportLauncher.launch("otter_backup_$stamp.json")
                },
                modifier = Modifier.weight(1f).testTag("export_backup_button"),
              ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.settings_export))
              }

              FilledTonalButton(
                onClick = {
                  importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.weight(1f).testTag("import_backup_button"),
              ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.settings_import))
              }
            }
          }
        }

        Spacer(Modifier.height(24.dp))
      }
    }
  }

  backupPreview?.let { preview ->
    AlertDialog(
      onDismissRequest = onDismissBackupPreview,
      title = { Text(stringResource(R.string.import_dialog_title)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            stringResource(
              R.string.import_dialog_date,
              DateTimeUtils.formatDateTime(preview.exportedAtMillis),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            stringResource(R.string.import_dialog_tasks, preview.taskCount),
            fontWeight = FontWeight.Medium,
          )
          Text(
            stringResource(R.string.import_dialog_attachments, preview.attachmentCount),
            fontWeight = FontWeight.Medium,
          )
          Text(
            stringResource(R.string.import_dialog_shopping, preview.shoppingItemCount),
            fontWeight = FontWeight.Medium,
          )
          if (preview.expenseCount > 0) {
            Text(
              stringResource(R.string.import_dialog_expenses, preview.expenseCount),
              fontWeight = FontWeight.Medium,
            )
          }
          Spacer(Modifier.height(6.dp))
          Text(
            stringResource(R.string.import_dialog_question),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { onConfirmImport(preview.payload, true) },
          modifier = Modifier.testTag("confirm_replace_import_button"),
        ) {
          Text(stringResource(R.string.import_replace_all))
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(onClick = onDismissBackupPreview) {
            Text(stringResource(R.string.action_cancel))
          }
          FilledTonalButton(
            onClick = { onConfirmImport(preview.payload, false) },
            modifier = Modifier.testTag("confirm_merge_import_button"),
          ) {
            Text(stringResource(R.string.import_merge))
          }
        }
      },
    )
  }
}
