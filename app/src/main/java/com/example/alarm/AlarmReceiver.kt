package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.OtterApp
import com.example.R
import com.example.data.model.AlarmSoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      AlarmContract.ACTION_TASK_ALARM -> {
        val serviceIntent =
          Intent(context, AlarmPlaybackService::class.java).apply { putExtras(intent) }
        ContextCompat.startForegroundService(context, serviceIntent)
      }

      AlarmContract.ACTION_TASK_REMINDER -> {
        val taskId = intent.getLongExtra(AlarmContract.EXTRA_TASK_ID, -1L)
        val title =
          intent.getStringExtra(AlarmContract.EXTRA_TITLE)
            ?: context.getString(R.string.default_task_name)
        val reminderLabel =
          intent.getStringExtra(AlarmContract.EXTRA_REMINDER_LABEL)
            ?: context.getString(R.string.reminder_on_time)
        val dueFormatted = intent.getStringExtra(AlarmContract.EXTRA_DUE_FORMATTED) ?: ""
        NotificationHelper.showEarlyReminderNotification(
          context = context,
          taskId = taskId,
          title = title,
          reminderLabel = reminderLabel,
          dueFormatted = dueFormatted,
        )
      }

      AlarmContract.ACTION_SNOOZE_10_MIN -> {
        val taskId = intent.getLongExtra(AlarmContract.EXTRA_TASK_ID, -1L)
        val title =
          intent.getStringExtra(AlarmContract.EXTRA_TITLE)
            ?: context.getString(R.string.alarm_default_title)
        val description = intent.getStringExtra(AlarmContract.EXTRA_DESCRIPTION) ?: ""
        val soundMode =
          intent.getStringExtra(AlarmContract.EXTRA_SOUND_MODE) ?: AlarmSoundMode.SYSTEM.name
        val soundUri = intent.getStringExtra(AlarmContract.EXTRA_SOUND_URI)
        val vibrate = intent.getBooleanExtra(AlarmContract.EXTRA_VIBRATE, true)

        context.stopService(Intent(context, AlarmPlaybackService::class.java))
        NotificationHelper.cancelRingingNotification(context, taskId)

        val newTrigger =
          AlarmScheduler.scheduleSnooze10Minutes(
            context = context,
            taskId = taskId,
            title = title,
            description = description,
            soundMode = soundMode,
            soundUri = soundUri,
            vibrate = vibrate,
          )

        if (taskId > 0L && newTrigger > 0L) {
          val pendingResult = goAsync()
          CoroutineScope(Dispatchers.IO).launch {
            runCatching {
              val app = context.applicationContext as OtterApp
              app.taskRepository.rescheduleTask(taskId, newTrigger)
            }
            pendingResult.finish()
          }
        }
      }

      AlarmContract.ACTION_STOP_ALARM -> {
        val taskId = intent.getLongExtra(AlarmContract.EXTRA_TASK_ID, -1L)
        context.stopService(Intent(context, AlarmPlaybackService::class.java))
        if (taskId >= 0L) {
          NotificationHelper.cancelRingingNotification(context, taskId)
        }
      }
    }
  }
}
