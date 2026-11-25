package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Transaction
  @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueAtMillis ASC")
  fun observeAllTasksWithAttachments(): Flow<List<TaskWithAttachments>>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
  suspend fun getTaskWithAttachmentsById(taskId: Long): TaskWithAttachments?

  @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND alarmEnabled = 1")
  suspend fun getPendingAlarmTasks(): List<TaskEntity>

  @Transaction
  @Query("SELECT * FROM tasks ORDER BY dueAtMillis ASC")
  suspend fun getAllTasksSnapshot(): List<TaskWithAttachments>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity): Long

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Delete
  suspend fun deleteTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :taskId")
  suspend fun deleteTaskById(taskId: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachments(attachments: List<TaskAttachmentEntity>)

  @Query("DELETE FROM task_attachments WHERE taskId = :taskId")
  suspend fun deleteAttachmentsForTask(taskId: Long)

  @Query("DELETE FROM tasks")
  suspend fun deleteAllTasks()
}
