package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithAttachments
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val database: AppDatabase) {
  private val taskDao = database.taskDao()

  val allTasks: Flow<List<TaskWithAttachments>> = taskDao.observeAllTasksWithAttachments()

  suspend fun getTaskById(taskId: Long): TaskWithAttachments? =
    taskDao.getTaskWithAttachmentsById(taskId)

  suspend fun getPendingAlarmTasks(): List<TaskEntity> = taskDao.getPendingAlarmTasks()

  suspend fun getAllTasksSnapshot(): List<TaskWithAttachments> = taskDao.getAllTasksSnapshot()

  suspend fun saveTask(task: TaskEntity, attachments: List<TaskAttachmentEntity>): Long =
    database.withTransaction {
      val savedId =
        if (task.id == 0L) {
          taskDao.insertTask(task)
        } else {
          taskDao.updateTask(task)
          task.id
        }
      taskDao.deleteAttachmentsForTask(savedId)
      if (attachments.isNotEmpty()) {
        taskDao.insertAttachments(attachments.map { it.copy(id = 0L, taskId = savedId) })
      }
      savedId
    }

  suspend fun completeTask(task: TaskEntity): TaskEntity? {
    return if (task.recurrence != RecurrenceType.NONE && !task.isCompleted) {
      val nextDue = DateTimeUtils.nextOccurrenceMillis(task.dueAtMillis, task.recurrence)
      val updated = task.copy(dueAtMillis = nextDue, isCompleted = false, completedAtMillis = null)
      taskDao.updateTask(updated)
      updated
    } else {
      val now = System.currentTimeMillis()
      val updated =
        if (task.isCompleted) {
          task.copy(isCompleted = false, completedAtMillis = null)
        } else {
          task.copy(isCompleted = true, completedAtMillis = now)
        }
      taskDao.updateTask(updated)
      updated
    }
  }

  suspend fun rescheduleTask(taskId: Long, newDueAtMillis: Long): TaskEntity? {
    val current = taskDao.getTaskWithAttachmentsById(taskId)?.task ?: return null
    val updated =
      current.copy(dueAtMillis = newDueAtMillis, isCompleted = false, completedAtMillis = null)
    taskDao.updateTask(updated)
    return updated
  }

  suspend fun deleteTask(task: TaskEntity) {
    taskDao.deleteTask(task)
  }

  suspend fun replaceAllTasks(
    items: List<Pair<TaskEntity, List<TaskAttachmentEntity>>>,
  ): List<TaskEntity> =
    database.withTransaction {
      taskDao.deleteAllTasks()
      val insertedTasks = mutableListOf<TaskEntity>()
      for ((task, attachments) in items) {
        val newId = taskDao.insertTask(task.copy(id = 0L))
        if (attachments.isNotEmpty()) {
          taskDao.insertAttachments(attachments.map { it.copy(id = 0L, taskId = newId) })
        }
        insertedTasks.add(task.copy(id = newId))
      }
      insertedTasks
    }

  suspend fun insertAdditionalTasks(
    items: List<Pair<TaskEntity, List<TaskAttachmentEntity>>>,
  ): List<TaskEntity> =
    database.withTransaction {
      val insertedTasks = mutableListOf<TaskEntity>()
      for ((task, attachments) in items) {
        val newId = taskDao.insertTask(task.copy(id = 0L))
        if (attachments.isNotEmpty()) {
          taskDao.insertAttachments(attachments.map { it.copy(id = 0L, taskId = newId) })
        }
        insertedTasks.add(task.copy(id = newId))
      }
      insertedTasks
    }
}
