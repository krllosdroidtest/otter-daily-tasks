package com.example.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.example.R

data class SystemSoundOption(
  val title: String,
  val uriString: String,
  val category: String,
)

object SystemSoundCatalog {
  fun loadSystemSounds(context: Context): List<SystemSoundOption> {
    val results = mutableListOf<SystemSoundOption>()

    val defaultAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    if (defaultAlarm != null) {
      results.add(
        SystemSoundOption(
          title = context.getString(R.string.sound_default_system),
          uriString = defaultAlarm.toString(),
          category = context.getString(R.string.sound_category_default),
        )
      )
    }
    val defaultNotif = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    if (defaultNotif != null) {
      results.add(
        SystemSoundOption(
          title = context.getString(R.string.sound_default_notification),
          uriString = defaultNotif.toString(),
          category = context.getString(R.string.sound_category_default),
        )
      )
    }

    collectType(
      context,
      RingtoneManager.TYPE_ALARM,
      context.getString(R.string.sound_category_alarm),
      results,
      limit = 12,
    )
    collectType(
      context,
      RingtoneManager.TYPE_NOTIFICATION,
      context.getString(R.string.sound_category_notification),
      results,
      limit = 12,
    )
    collectType(
      context,
      RingtoneManager.TYPE_RINGTONE,
      context.getString(R.string.sound_category_ringtone),
      results,
      limit = 8,
    )

    return results.distinctBy { it.uriString }
  }

  private fun collectType(
    context: Context,
    type: Int,
    category: String,
    out: MutableList<SystemSoundOption>,
    limit: Int,
  ) {
    runCatching {
      val manager = RingtoneManager(context).apply { setType(type) }
      val cursor = manager.cursor ?: return
      var count = 0
      cursor.use {
        while (it.moveToNext() && count < limit) {
          val title = it.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
          val uri: Uri = manager.getRingtoneUri(it.position) ?: continue
          out.add(SystemSoundOption(title = title, uriString = uri.toString(), category = category))
          count++
        }
      }
    }
  }

  fun resolveSoundTitle(context: Context, uri: Uri?): String {
    if (uri == null) return context.getString(R.string.sound_default_system)
    return runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
      ?: uri.lastPathSegment
      ?: context.getString(R.string.sound_selected_fallback)
  }
}
