package com.example.alarm

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.OtterApp
import com.example.R
import com.example.data.model.AlarmSoundMode
import com.example.data.repository.ThemePreferences
import com.example.ui.theme.OtterTheme

class AlarmAlertActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val taskId = intent.getLongExtra(AlarmContract.EXTRA_TASK_ID, -1L)
    val title =
      intent.getStringExtra(AlarmContract.EXTRA_TITLE) ?: getString(R.string.alarm_default_title)
    val description = intent.getStringExtra(AlarmContract.EXTRA_DESCRIPTION) ?: ""
    val soundMode =
      intent.getStringExtra(AlarmContract.EXTRA_SOUND_MODE) ?: AlarmSoundMode.SYSTEM.name
    val soundUri = intent.getStringExtra(AlarmContract.EXTRA_SOUND_URI)
    val vibrate = intent.getBooleanExtra(AlarmContract.EXTRA_VIBRATE, true)

    val app = application as OtterApp

    setContent {
      val themePrefs by
        app.themePreferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
          initialValue = ThemePreferences()
        )

      OtterTheme(
        themeMode = themePrefs.themeMode,
        useDynamicColor = themePrefs.useDynamicColor,
        palette = themePrefs.palette,
      ) {
        val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
        val scale by
          pulseTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.1f,
            animationSpec =
              infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse,
              ),
            label = "alarm_scale",
          )

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(120.dp)
                    .scale(scale)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Alarm,
                  contentDescription = stringResource(R.string.alarm_running_text),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(64.dp),
                )
              }

              Text(
                text = stringResource(R.string.alarm_default_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )

              Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
              )

              if (description.isNotBlank()) {
                Text(
                  text = description,
                  style = MaterialTheme.typography.bodyLarge,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                FilledTonalButton(
                  onClick = {
                    val snoozeIntent =
                      Intent(this@AlarmAlertActivity, AlarmReceiver::class.java).apply {
                        action = AlarmContract.ACTION_SNOOZE_10_MIN
                        putExtra(AlarmContract.EXTRA_TASK_ID, taskId)
                        putExtra(AlarmContract.EXTRA_TITLE, title)
                        putExtra(AlarmContract.EXTRA_DESCRIPTION, description)
                        putExtra(AlarmContract.EXTRA_SOUND_MODE, soundMode)
                        putExtra(AlarmContract.EXTRA_SOUND_URI, soundUri)
                        putExtra(AlarmContract.EXTRA_VIBRATE, vibrate)
                      }
                    sendBroadcast(snoozeIntent)
                    Toast.makeText(
                        this@AlarmAlertActivity,
                        getString(R.string.alarm_snoozed_toast),
                        Toast.LENGTH_SHORT,
                      )
                      .show()
                    finish()
                  },
                  modifier =
                    Modifier.weight(1f).height(56.dp).testTag("alarm_alert_snooze_button"),
                ) {
                  Icon(Icons.Rounded.Snooze, contentDescription = null)
                  Spacer(Modifier.size(8.dp))
                  Text(stringResource(R.string.action_snooze_10))
                }

                Button(
                  onClick = {
                    val stopIntent =
                      Intent(this@AlarmAlertActivity, AlarmReceiver::class.java).apply {
                        action = AlarmContract.ACTION_STOP_ALARM
                        putExtra(AlarmContract.EXTRA_TASK_ID, taskId)
                      }
                    sendBroadcast(stopIntent)
                    finish()
                  },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.error,
                      contentColor = MaterialTheme.colorScheme.onError,
                    ),
                  modifier =
                    Modifier.weight(1f).height(56.dp).testTag("alarm_alert_stop_button"),
                ) {
                  Icon(Icons.Rounded.AlarmOff, contentDescription = null)
                  Spacer(Modifier.size(8.dp))
                  Text(stringResource(R.string.action_stop_alarm))
                }
              }
            }
          }
        }
      }
    }
  }
}
