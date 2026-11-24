package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.model.TaskEntity
import com.example.util.DateTimeUtils

object AlarmContract {
  const val ACTION_TASK_ALARM = "com.example.alarm.ACTION_TASK_ALARM"
  const val ACTION_TASK_REMINDER = "com.example.alarm.ACTION_TASK_REMINDER"
  const val ACTION_SNOOZE_10_MIN = "com.example.alarm.ACTION_SNOOZE_10_MIN"
  const val ACTION_STOP_ALARM = "com.example.alarm.ACTION_STOP_ALARM"

  const val EXTRA_TASK_ID = "extra_task_id"
  const val EXTRA_TITLE = "extra_title"
  const val EXTRA_DESCRIPTION = "extra_description"
  const val EXTRA_SOUND_MODE = "extra_sound_mode"
  const val EXTRA_SOUND_URI = "extra_sound_uri"
  const val EXTRA_VIBRATE = "extra_vibrate"
  const val EXTRA_REMINDER_LABEL = "extra_reminder_label"
  const val EXTRA_DUE_FORMATTED = "extra_due_formatted"
}

object AlarmScheduler {
  fun canScheduleExactAlarms(context: Context): Boolean {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
  }

  fun scheduleTask(context: Context, task: TaskEntity) {
    cancelTask(context, task.id)
    if (task.isCompleted || !task.alarmEnabled) return

    val now = System.currentTimeMillis()
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

    // 1. Main task alarm
    if (task.dueAtMillis > now - 15_000L) {
      val triggerAt = maxOf(task.dueAtMillis, now + 1_000L)
      val alarmPi = buildAlarmPendingIntent(context, task)
      scheduleExactOrFallback(context, alarmManager, triggerAt, alarmPi, useAlarmClock = true)
    }

    // 2. Early reminder (up to 1 week before)
    if (task.reminderOffsetMinutes > 0) {
      val reminderAt = task.dueAtMillis - task.reminderOffsetMinutes * 60_000L
      if (reminderAt > now) {
        val reminderPi = buildReminderPendingIntent(context, task)
        scheduleExactOrFallback(context, alarmManager, reminderAt, reminderPi, useAlarmClock = false)
      }
    }
  }

  fun scheduleSnooze10Minutes(
    context: Context,
    taskId: Long,
    title: String,
    description: String,
    soundMode: String,
    soundUri: String?,
    vibrate: Boolean,
  ): Long {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return 0L
    val triggerAt = System.currentTimeMillis() + 10 * 60_000L
    val intent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmContract.ACTION_TASK_ALARM
        putExtra(AlarmContract.EXTRA_TASK_ID, taskId)
        putExtra(AlarmContract.EXTRA_TITLE, title)
        putExtra(AlarmContract.EXTRA_DESCRIPTION, description)
        putExtra(AlarmContract.EXTRA_SOUND_MODE, soundMode)
        putExtra(AlarmContract.EXTRA_SOUND_URI, soundUri)
        putExtra(AlarmContract.EXTRA_VIBRATE, vibrate)
      }
    val pi =
      PendingIntent.getBroadcast(
        context,
        mainRequestCode(taskId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    scheduleExactOrFallback(context, alarmManager, triggerAt, pi, useAlarmClock = true)
    return triggerAt
  }

  fun cancelTask(context: Context, taskId: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val alarmIntent =
      Intent(context, AlarmReceiver::class.java).apply { action = AlarmContract.ACTION_TASK_ALARM }
    val alarmPi =
      PendingIntent.getBroadcast(
        context,
        mainRequestCode(taskId),
        alarmIntent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
    if (alarmPi != null) {
      alarmManager.cancel(alarmPi)
      alarmPi.cancel()
    }

    val remIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmContract.ACTION_TASK_REMINDER
      }
    val remPi =
      PendingIntent.getBroadcast(
        context,
        reminderRequestCode(taskId),
        remIntent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
    if (remPi != null) {
      alarmManager.cancel(remPi)
      remPi.cancel()
    }
  }

  private fun scheduleExactOrFallback(
    context: Context,
    alarmManager: AlarmManager,
    triggerAtMillis: Long,
    operation: PendingIntent,
    useAlarmClock: Boolean,
  ) {
    val canExact = canScheduleExactAlarms(context)
    if (canExact) {
      if (useAlarmClock) {
        val showIntent =
          PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
          )
        alarmManager.setAlarmClock(
          AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
          operation,
        )
      } else {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          operation,
        )
      }
    } else {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    }
  }

  private fun buildAlarmPendingIntent(context: Context, task: TaskEntity): PendingIntent {
    val intent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmContract.ACTION_TASK_ALARM
        putExtra(AlarmContract.EXTRA_TASK_ID, task.id)
        putExtra(AlarmContract.EXTRA_TITLE, task.title)
        putExtra(AlarmContract.EXTRA_DESCRIPTION, task.description)
        putExtra(AlarmContract.EXTRA_SOUND_MODE, task.alarmSoundMode.name)
        putExtra(AlarmContract.EXTRA_SOUND_URI, task.alarmSoundUri)
        putExtra(AlarmContract.EXTRA_VIBRATE, task.vibrate)
      }
    return PendingIntent.getBroadcast(
      context,
      mainRequestCode(task.id),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun buildReminderPendingIntent(context: Context, task: TaskEntity): PendingIntent {
    val intent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmContract.ACTION_TASK_REMINDER
        putExtra(AlarmContract.EXTRA_TASK_ID, task.id)
        putExtra(AlarmContract.EXTRA_TITLE, task.title)
        putExtra(
          AlarmContract.EXTRA_REMINDER_LABEL,
          DateTimeUtils.formatReminderOffset(context, task.reminderOffsetMinutes),
        )
        putExtra(AlarmContract.EXTRA_DUE_FORMATTED, DateTimeUtils.formatDateTime(task.dueAtMillis))
      }
    return PendingIntent.getBroadcast(
      context,
      reminderRequestCode(task.id),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun mainRequestCode(taskId: Long): Int = (taskId.toInt() * 2) + 100

  private fun reminderRequestCode(taskId: Long): Int = (taskId.toInt() * 2) + 101
}
