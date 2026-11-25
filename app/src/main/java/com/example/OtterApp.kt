package com.example

import android.app.Application
import com.example.alarm.NotificationHelper
import com.example.data.db.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.ThemePreferencesRepository

class OtterApp : Application() {
  lateinit var database: AppDatabase
    private set

  lateinit var taskRepository: TaskRepository
    private set

  lateinit var shoppingRepository: ShoppingRepository
    private set

  lateinit var expenseRepository: ExpenseRepository
    private set

  lateinit var themePreferencesRepository: ThemePreferencesRepository
    private set

  override fun onCreate() {
    super.onCreate()
    NotificationHelper.ensureChannels(this)
    database = AppDatabase.getInstance(this)
    taskRepository = TaskRepository(database)
    shoppingRepository = ShoppingRepository(database)
    expenseRepository = ExpenseRepository(database)
    themePreferencesRepository = ThemePreferencesRepository(this)
  }
}
