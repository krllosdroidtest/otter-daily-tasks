package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.TaskAttachmentEntity
import java.io.File
import java.util.UUID

data class LocalCopiedFile(
  val displayName: String,
  val mimeType: String,
  val localFilePath: String,
  val sizeBytes: Long,
)

object AttachmentStorage {
  private const val ATTACHMENTS_DIR = "attachments"
  private const val SOUNDS_DIR = "alarm_sounds"

  fun copyUriToAttachments(context: Context, uri: Uri): LocalCopiedFile? =
    copyUriToDir(context, uri, ATTACHMENTS_DIR)

  fun copyUriToSounds(context: Context, uri: Uri): LocalCopiedFile? =
    copyUriToDir(context, uri, SOUNDS_DIR)

  private fun copyUriToDir(context: Context, uri: Uri, dirName: String): LocalCopiedFile? {
    return runCatching {
        val resolver = context.contentResolver
        var displayName = "arquivo_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (cursor.moveToFirst()) {
            if (nameIdx >= 0) {
              cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
            }
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
              sizeBytes = cursor.getLong(sizeIdx)
            }
          }
        }

        val mimeType =
          resolver.getType(uri)
            ?: MimeTypeMap.getSingleton()
              .getMimeTypeFromExtension(displayName.substringAfterLast('.', ""))
            ?: "application/octet-stream"

        val targetDir = File(context.filesDir, dirName).apply { mkdirs() }
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(targetDir, "${UUID.randomUUID().toString().take(8)}_$safeName")

        resolver.openInputStream(uri)?.use { input ->
          destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        val actualSize = if (sizeBytes > 0L) sizeBytes else destFile.length()
        LocalCopiedFile(
          displayName = displayName,
          mimeType = mimeType,
          localFilePath = destFile.absolutePath,
          sizeBytes = actualSize,
        )
      }
      .getOrNull()
  }

  fun writeBytesToAttachments(
    context: Context,
    displayName: String,
    bytes: ByteArray,
  ): String {
    val targetDir = File(context.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
    val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val destFile = File(targetDir, "${UUID.randomUUID().toString().take(8)}_$safeName")
    destFile.writeBytes(bytes)
    return destFile.absolutePath
  }

  fun openAttachment(context: Context, attachment: TaskAttachmentEntity): Boolean {
    val file = File(attachment.localFilePath)
    if (!file.exists()) return false
    return runCatching {
        val uri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
          Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        context.startActivity(
          Intent.createChooser(
            intent,
            context.getString(com.example.R.string.task_open_attachment_chooser, attachment.displayName),
          )
        )
        true
      }
      .getOrDefault(false)
  }

  fun formatSize(bytes: Long): String =
    when {
      bytes < 1024 -> "$bytes B"
      bytes < 1024 * 1024 -> "${bytes / 1024} KB"
      else -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
