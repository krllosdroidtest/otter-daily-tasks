package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
  const val ALARM_CHANNEL_ID = "otter_alarm_channel"
  const val REMINDER_CHANNEL_ID = "otter_reminder_channel"
  const val ALARM_NOTIFICATION_ID_BASE = 20000
  const val REMINDER_NOTIFICATION_ID_BASE = 40000

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java) ?: return

      val alarmChannel =
        NotificationChannel(
            ALARM_CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          )
          .apply {
            description = context.getString(R.string.alarm_channel_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
          }

      val reminderChannel =
        NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          )
          .apply {
            description = context.getString(R.string.reminder_channel_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
          }

      manager.createNotificationChannel(alarmChannel)
      manager.createNotificationChannel(reminderChannel)
    }
  }

  fun buildRingingAlarmNotification(
    context: Context,
    taskId: Long,
    title: String,
    description: String,
    soundMode: String,
    soundUri: String?,
    vibrate: Boolean,
  ): Notification {
    ensureChannels(context)

    val openIntent =
      Intent(context, AlarmAlertActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(AlarmContract.EXTRA_TASK_ID, taskId)
        putExtra(AlarmContract.EXTRA_TITLE, title)
        putExtra(AlarmContract.EXTRA_DESCRIPTION, description)
        putExtra(AlarmContract.EXTRA_SOUND_MODE, soundMode)
        putExtra(AlarmContract.EXTRA_SOUND_URI, soundUri)
        putExtra(AlarmContract.EXTRA_VIBRATE, vibrate)
      }

    val openPendingIntent =
      PendingIntent.getActivity(
        context,
        taskId.toInt() + 1000,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val snoozeIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmContract.ACTION_SNOOZE_10_MIN
        putExtra(AlarmContract.EXTRA_TASK_ID, taskId)
        putExtra(AlarmContract.EXTRA_TITLE, title)
        putExtra(AlarmContract.EXTRA_DESCRIPTION, description)
        putExtra(AlarmContract.EXTRA_SOUND_MODE, soundMode)
        putExtra(AlarmContract.EXTRA_SOUND_URI, soundUri)
        putExtra(AlarmContract.EXTRA_VIBRATE, vibrate)
      }

    val snoozePendingIntent =
      PendingIntent.getBroadcast(
        context,
        taskId.toInt() + 2000,
        snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmContract.ACTION_STOP_ALARM
        putExtra(AlarmContract.EXTRA_TASK_ID, taskId)
      }

    val stopPendingIntent =
      PendingIntent.getBroadcast(
        context,
        taskId.toInt() + 3000,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val bodyText =
      if (description.isNotBlank()) description else context.getString(R.string.alarm_running_text)

    return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle(title.ifBlank { context.getString(R.string.alarm_default_title) })
      .setContentText(bodyText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setContentIntent(openPendingIntent)
      .addAction(
        android.R.drawable.ic_popup_reminder,
        context.getString(R.string.action_snooze_10),
        snoozePendingIntent,
      )
      .addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        context.getString(R.string.action_stop_alarm),
        stopPendingIntent,
      )
      .build()
  }

  fun showEarlyReminderNotification(
    context: Context,
    taskId: Long,
    title: String,
    reminderLabel: String,
    dueFormatted: String,
  ) {
    ensureChannels(context)
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val openMainIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val openPendingIntent =
      PendingIntent.getActivity(
        context,
        taskId.toInt() + 4000,
        openMainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val text = context.getString(R.string.reminder_body, reminderLabel, dueFormatted)
    val notification =
      NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(context.getString(R.string.reminder_title_prefix, title))
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(openPendingIntent)
        .build()

    runCatching { manager.notify(REMINDER_NOTIFICATION_ID_BASE + taskId.toInt(), notification) }
  }

  fun cancelRingingNotification(context: Context, taskId: Long) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.cancel(ALARM_NOTIFICATION_ID_BASE + taskId.toInt())
  }
}
