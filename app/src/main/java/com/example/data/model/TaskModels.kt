package com.example.data.model

import androidx.annotation.StringRes
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.R

enum class RecurrenceType(@StringRes val labelRes: Int) {
  NONE(R.string.recurrence_none),
  DAILY(R.string.recurrence_daily),
  WEEKLY(R.string.recurrence_weekly),
  MONTHLY(R.string.recurrence_monthly),
  YEARLY(R.string.recurrence_yearly),
}

enum class AlarmSoundMode(@StringRes val labelRes: Int) {
  SYSTEM(R.string.sound_mode_system),
  MANUAL(R.string.sound_mode_manual),
  SILENT(R.string.sound_mode_silent),
}

@Entity(tableName = "tasks")
data class TaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val description: String = "",
  val dueAtMillis: Long,
  val recurrence: RecurrenceType = RecurrenceType.NONE,
  val reminderOffsetMinutes: Int = 0, // 0..10080 (up to 1 week before)
  val alarmEnabled: Boolean = true,
  val alarmSoundMode: AlarmSoundMode = AlarmSoundMode.SYSTEM,
  val alarmSoundUri: String? = null,
  val alarmSoundTitle: String = "",
  val vibrate: Boolean = true,
  val isCompleted: Boolean = false,
  val completedAtMillis: Long? = null,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "task_attachments",
  foreignKeys =
    [
      ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["taskId"])],
)
data class TaskAttachmentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val taskId: Long,
  val displayName: String,
  val mimeType: String,
  val localFilePath: String,
  val sizeBytes: Long,
)

data class TaskWithAttachments(
  @Embedded val task: TaskEntity,
  @Relation(parentColumn = "id", entityColumn = "taskId")
  val attachments: List<TaskAttachmentEntity> = emptyList(),
)

@Entity(tableName = "shopping_folders")
data class ShoppingFolderEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "shopping_items",
  foreignKeys =
    [
      ForeignKey(
        entity = ShoppingFolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["folderId"])],
)
data class ShoppingItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val folderId: Long? = null,
  val name: String,
  val quantity: Int = 1,
  val isChecked: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
