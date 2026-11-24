package com.example.alarm

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.AlarmSoundMode
import java.io.File

class AlarmPlaybackService : Service() {
  private var mediaPlayer: MediaPlayer? = null
  private var vibrator: Vibrator? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var activeTaskId: Long = -1L

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == AlarmContract.ACTION_STOP_ALARM) {
      stopSelf()
      return START_NOT_STICKY
    }

    val taskId = intent?.getLongExtra(AlarmContract.EXTRA_TASK_ID, -1L) ?: -1L
    val title =
      intent?.getStringExtra(AlarmContract.EXTRA_TITLE)
        ?: getString(com.example.R.string.alarm_default_title)
    val description = intent?.getStringExtra(AlarmContract.EXTRA_DESCRIPTION) ?: ""
    val soundModeName =
      intent?.getStringExtra(AlarmContract.EXTRA_SOUND_MODE) ?: AlarmSoundMode.SYSTEM.name
    val soundUriStr = intent?.getStringExtra(AlarmContract.EXTRA_SOUND_URI)
    val vibrate = intent?.getBooleanExtra(AlarmContract.EXTRA_VIBRATE, true) ?: true

    activeTaskId = taskId

    val notification =
      NotificationHelper.buildRingingAlarmNotification(
        context = this,
        taskId = taskId,
        title = title,
        description = description,
        soundMode = soundModeName,
        soundUri = soundUriStr,
        vibrate = vibrate,
      )

    val notificationId = NotificationHelper.ALARM_NOTIFICATION_ID_BASE + taskId.toInt()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        notificationId,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
      )
    } else {
      startForeground(notificationId, notification)
    }

    stopPlayback()
    acquireWakeLock()

    val soundMode =
      runCatching { AlarmSoundMode.valueOf(soundModeName) }.getOrDefault(AlarmSoundMode.SYSTEM)
    if (soundMode != AlarmSoundMode.SILENT) {
      startAudio(soundUriStr)
    }
    if (vibrate) {
      startVibration()
    }

    return START_NOT_STICKY
  }

  private fun acquireWakeLock() {
    runCatching {
      val pm = getSystemService(PowerManager::class.java) ?: return
      val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "otter::AlarmWakeLock")
      lock.acquire(10 * 60_000L) // Safety timeout: 10 minutes
      wakeLock = lock
    }
  }

  private fun startAudio(soundUriStr: String?) {
    val audioAttrs =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val candidateUris = buildList {
      if (!soundUriStr.isNullOrBlank()) {
        if (soundUriStr.startsWith("/")) {
          val file = File(soundUriStr)
          if (file.exists()) add(Uri.fromFile(file))
        } else {
          add(Uri.parse(soundUriStr))
        }
      }
      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { add(it) }
      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { add(it) }
    }

    for (uri in candidateUris) {
      val started =
        runCatching {
            val player = MediaPlayer()
            player.setAudioAttributes(audioAttrs)
            player.setDataSource(this, uri)
            player.isLooping = true
            player.prepare()
            player.start()
            mediaPlayer = player
            true
          }
          .getOrDefault(false)
      if (started) break
    }
  }

  @Suppress("DEPRECATION")
  private fun startVibration() {
    val vib =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
      } else {
        getSystemService(VIBRATOR_SERVICE) as? Vibrator
      }
    vibrator = vib
    if (vib?.hasVibrator() != true) return

    val pattern = longArrayOf(0, 500, 400, 500, 800)
    val amplitudes = intArrayOf(0, 255, 0, 255, 0)

    runCatching {
      when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
          val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
          val vibAttrs =
            VibrationAttributes.Builder()
              .setUsage(VibrationAttributes.USAGE_ALARM)
              .build()
          vib.vibrate(effect, vibAttrs)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
          val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
          val audioAttrs =
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          vib.vibrate(effect, audioAttrs)
        }
        else -> {
          val audioAttrs =
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          vib.vibrate(pattern, 0, audioAttrs)
        }
      }
    }
  }

  private fun stopPlayback() {
    runCatching {
      mediaPlayer?.let {
        if (it.isPlaying) it.stop()
        it.release()
      }
    }
    mediaPlayer = null
    runCatching { vibrator?.cancel() }
    vibrator = null
    runCatching {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
    }
    wakeLock = null
  }

  override fun onDestroy() {
    stopPlayback()
    if (activeTaskId >= 0L) {
      NotificationHelper.cancelRingingNotification(this, activeTaskId)
    }
    super.onDestroy()
  }
}
