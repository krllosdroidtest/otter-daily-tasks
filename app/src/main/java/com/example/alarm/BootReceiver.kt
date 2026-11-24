package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.OtterApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (
      action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
    ) {
      val pendingResult = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        runCatching {
          val app = context.applicationContext as OtterApp
          val tasks = app.taskRepository.getPendingAlarmTasks()
          tasks.forEach { AlarmScheduler.scheduleTask(context, it) }
        }
        pendingResult.finish()
      }
    }
  }
}
