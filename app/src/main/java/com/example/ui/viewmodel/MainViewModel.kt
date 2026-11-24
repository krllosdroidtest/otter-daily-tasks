package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.OtterApp
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.backup.BackupManager
import com.example.backup.BackupPayload
import com.example.backup.BackupPreview
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ShoppingFolderEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithAttachments
import com.example.data.repository.ThemePreferences
import com.example.ui.theme.AppPalette
import com.example.ui.theme.ThemeMode
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import com.example.util.ExpenseCalculator
import com.example.util.ExpenseOccurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TaskFilter(@StringRes val labelRes: Int) {
  SELECTED_DAY(R.string.filter_selected_day),
  ALL_PENDING(R.string.filter_all_pending),
  COMPLETED(R.string.filter_completed),
}

data class CalendarTaskItem(
  val taskWithAttachments: TaskWithAttachments,
  val displayDueAtMillis: Long,
  val isFutureProjection: Boolean,
)

data class TasksUiState(
  val selectedDay: DayKey = DayKey.today(),
  val displayedYear: Int = DayKey.today().year,
  val displayedMonth: Int = DayKey.today().monthZeroBased,
  val isMonthExpanded: Boolean = true,
  val filter: TaskFilter = TaskFilter.SELECTED_DAY,
  val allTasks: List<TaskWithAttachments> = emptyList(),
  val visibleTasks: List<CalendarTaskItem> = emptyList(),
  val taskCountByDay: Map<DayKey, Int> = emptyMap(),
)

data class ShoppingUiState(
  val folders: List<ShoppingFolderEntity> = emptyList(),
  val selectedFolderId: Long? = null,
  val allItems: List<ShoppingItemEntity> = emptyList(),
  val visibleItems: List<ShoppingItemEntity> = emptyList(),
  val itemCountByFolder: Map<Long?, Int> = emptyMap(),
)

enum class ExpenseFilter(@StringRes val labelRes: Int) {
  SELECTED_DAY(R.string.expense_filter_day),
  MONTH(R.string.expense_filter_month),
  INSTALLMENTS(R.string.expense_filter_installments),
  RECURRING(R.string.expense_filter_recurring),
}

data class CategorySpendSlice(
  val category: ExpenseCategoryEntity?,
  val totalCents: Long,
  val fraction: Float,
)

data class ExpensesUiState(
  val selectedDay: DayKey = DayKey.today(),
  val displayedYear: Int = DayKey.today().year,
  val displayedMonth: Int = DayKey.today().monthZeroBased,
  val isMonthExpanded: Boolean = true,
  val filter: ExpenseFilter = ExpenseFilter.SELECTED_DAY,
  val selectedCategoryFilterId: Long? = null,
  val categories: List<ExpenseCategoryEntity> = emptyList(),
  val visibleOccurrences: List<ExpenseOccurrence> = emptyList(),
  val installmentPlans: List<ExpenseEntity> = emptyList(),
  val recurringPlans: List<ExpenseEntity> = emptyList(),
  val paidKeys: Set<Pair<Long, String>> = emptySet(),
  val expenseCountByDay: Map<DayKey, Int> = emptyMap(),
  val monthTotalCents: Long = 0L,
  val monthOneTimeCents: Long = 0L,
  val monthInstallmentCents: Long = 0L,
  val monthRecurringCents: Long = 0L,
  val dayTotalCents: Long = 0L,
  val categoryBreakdown: List<CategorySpendSlice> = emptyList(),
  val dailyTotalsInMonth: Map<Int, Long> = emptyMap(),
)

private data class ExpenseCalendarControlState(
  val selectedDay: DayKey,
  val displayedYearMonth: Pair<Int, Int>,
  val isMonthExpanded: Boolean,
  val filter: ExpenseFilter,
  val selectedCategoryFilterId: Long?,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as OtterApp
  private val taskRepo = app.taskRepository
  private val shoppingRepo = app.shoppingRepository
  private val expenseRepo = app.expenseRepository
  private val themeRepo = app.themePreferencesRepository

  // Tasks state
  private val _selectedDay = MutableStateFlow(DayKey.today())
  private val _displayedYearMonth =
    MutableStateFlow(DayKey.today().year to DayKey.today().monthZeroBased)
  private val _isMonthExpanded = MutableStateFlow(true)
  private val _taskFilter = MutableStateFlow(TaskFilter.SELECTED_DAY)

  // Shopping folders state
  private val _selectedFolderId = MutableStateFlow<Long?>(null)

  // Expenses state
  private val _expenseSelectedDay = MutableStateFlow(DayKey.today())
  private val _expenseDisplayedYearMonth =
    MutableStateFlow(DayKey.today().year to DayKey.today().monthZeroBased)
  private val _expenseMonthExpanded = MutableStateFlow(true)
  private val _expenseFilter = MutableStateFlow(ExpenseFilter.SELECTED_DAY)
  private val _expenseCategoryFilterId = MutableStateFlow<Long?>(null)

  private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
  val backupPreview: StateFlow<BackupPreview?> = _backupPreview

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage

  val themePreferences: StateFlow<ThemePreferences> =
    themeRepo.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ThemePreferences(),
    )

  val expenseCategories: StateFlow<List<ExpenseCategoryEntity>> =
    expenseRepo.allCategories.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val shoppingUiState: StateFlow<ShoppingUiState> =
    combine(shoppingRepo.allFolders, shoppingRepo.allItems, _selectedFolderId) {
        folders,
        items,
        selectedFolder ->
        val validFolderId =
          selectedFolder?.takeIf { id -> folders.any { folder -> folder.id == id } }
        val counts = items.groupingBy { it.folderId }.eachCount()
        val visible = items.filter { it.folderId == validFolderId }
        ShoppingUiState(
          folders = folders,
          selectedFolderId = validFolderId,
          allItems = items,
          visibleItems = visible,
          itemCountByFolder = counts,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShoppingUiState(),
      )

  val tasksUiState: StateFlow<TasksUiState> =
    combine(
        taskRepo.allTasks,
        _selectedDay,
        _displayedYearMonth,
        _isMonthExpanded,
        _taskFilter,
      ) { tasks, selectedDay, yearMonth, isExpanded, filter ->
        val (monthStart, monthEnd) =
          DateTimeUtils.monthRangeWithWeekBuffer(yearMonth.first, yearMonth.second)

        val pendingCountMap = mutableMapOf<DayKey, Int>()
        for (item in tasks) {
          if (!item.task.isCompleted) {
            val occurrences =
              DateTimeUtils.generateOccurrencesInRange(item.task, monthStart, monthEnd)
            for (occMillis in occurrences) {
              val key = DayKey.fromMillis(occMillis)
              pendingCountMap[key] = (pendingCountMap[key] ?: 0) + 1
            }
          }
        }

        val visible =
          when (filter) {
            TaskFilter.SELECTED_DAY -> {
              val dayStart = selectedDay.startOfDayMillis()
              val dayEnd = selectedDay.endOfDayMillis()
              tasks
                .flatMap { item ->
                  val t = item.task
                  val isBaseDay = DayKey.fromMillis(t.dueAtMillis) == selectedDay
                  if (isBaseDay) {
                    listOf(
                      CalendarTaskItem(
                        taskWithAttachments = item,
                        displayDueAtMillis = t.dueAtMillis,
                        isFutureProjection = false,
                      )
                    )
                  } else if (!t.isCompleted && t.recurrence != RecurrenceType.NONE) {
                    DateTimeUtils.generateOccurrencesInRange(t, dayStart, dayEnd)
                      .filter { it > t.dueAtMillis }
                      .map { projectedMillis ->
                        CalendarTaskItem(
                          taskWithAttachments = item,
                          displayDueAtMillis = projectedMillis,
                          isFutureProjection = true,
                        )
                      }
                  } else {
                    emptyList()
                  }
                }
                .sortedWith(
                  compareBy<CalendarTaskItem> { it.taskWithAttachments.task.isCompleted }
                    .thenBy { it.displayDueAtMillis }
                )
            }
            TaskFilter.ALL_PENDING ->
              tasks
                .filter { !it.task.isCompleted }
                .map { CalendarTaskItem(it, it.task.dueAtMillis, isFutureProjection = false) }
            TaskFilter.COMPLETED ->
              tasks
                .filter { it.task.isCompleted }
                .map { CalendarTaskItem(it, it.task.dueAtMillis, isFutureProjection = false) }
          }

        TasksUiState(
          selectedDay = selectedDay,
          displayedYear = yearMonth.first,
          displayedMonth = yearMonth.second,
          isMonthExpanded = isExpanded,
          filter = filter,
          allTasks = tasks,
          visibleTasks = visible,
          taskCountByDay = pendingCountMap,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState(),
      )

  private val expenseControlState =
    combine(
      _expenseSelectedDay,
      _expenseDisplayedYearMonth,
      _expenseMonthExpanded,
      _expenseFilter,
      _expenseCategoryFilterId,
    ) { day, yearMonth, expanded, filter, catFilter ->
      ExpenseCalendarControlState(day, yearMonth, expanded, filter, catFilter)
    }

  val expensesUiState: StateFlow<ExpensesUiState> =
    combine(
        expenseRepo.allCategories,
        expenseRepo.allExpenses,
        expenseRepo.allPayments,
        expenseControlState,
      ) { categories, expenses, payments, ctrl ->
        val validCatFilter =
          ctrl.selectedCategoryFilterId?.takeIf { id -> categories.any { it.id == id } }
        val filteredExpenses =
          if (validCatFilter == null) expenses
          else expenses.filter { it.categoryId == validCatFilter }

        val paidKeys = payments.map { it.expenseId to it.occurrenceKey }.toSet()

        val year = ctrl.displayedYearMonth.first
        val month = ctrl.displayedYearMonth.second

        val monthOccurrences =
          ExpenseCalculator.occurrencesInMonth(filteredExpenses, year, month, paidKeys)
        val windowOccurrences =
          ExpenseCalculator.occurrencesInCalendarWindow(filteredExpenses, year, month, paidKeys)

        val countMap = mutableMapOf<DayKey, Int>()
        for (occ in windowOccurrences) {
          countMap[occ.dayKey] = (countMap[occ.dayKey] ?: 0) + 1
        }

        val monthTotal = monthOccurrences.sumOf { it.amountCents }
        val monthOneTime =
          monthOccurrences.filter { !it.isInstallment && !it.isRecurring }.sumOf { it.amountCents }
        val monthInstallment =
          monthOccurrences.filter { it.isInstallment }.sumOf { it.amountCents }
        val monthRecurring =
          monthOccurrences.filter { it.isRecurring }.sumOf { it.amountCents }

        val selectedDayOccurrences =
          ExpenseCalculator.occurrencesInMonth(
              filteredExpenses,
              ctrl.selectedDay.year,
              ctrl.selectedDay.monthZeroBased,
              paidKeys,
            )
            .filter { it.dayKey == ctrl.selectedDay }
        val dayTotal = selectedDayOccurrences.sumOf { it.amountCents }

        val categoryById = categories.associateBy { it.id }
        val breakdown =
          if (monthTotal <= 0L) {
            emptyList()
          } else {
            monthOccurrences
              .groupBy { occ -> occ.expense.categoryId?.takeIf { categoryById.containsKey(it) } }
              .map { (catId, list) ->
                val sum = list.sumOf { it.amountCents }
                CategorySpendSlice(
                  category = catId?.let { categoryById[it] },
                  totalCents = sum,
                  fraction = sum.toFloat() / monthTotal.toFloat(),
                )
              }
              .sortedByDescending { it.totalCents }
          }

        val dailyTotals =
          monthOccurrences
            .groupBy { it.dayKey.dayOfMonth }
            .mapValues { (_, list) -> list.sumOf { it.amountCents } }

        val visibleOccurrences =
          when (ctrl.filter) {
            ExpenseFilter.SELECTED_DAY -> selectedDayOccurrences
            ExpenseFilter.MONTH -> monthOccurrences
            ExpenseFilter.INSTALLMENTS,
            ExpenseFilter.RECURRING -> emptyList()
          }

        val installmentPlans =
          filteredExpenses
            .filter { it.isInstallment && it.installmentCount > 1 }
            .sortedByDescending { it.startYear * 12 + it.startMonthZeroBased }

        val recurringPlans =
          filteredExpenses
            .filter { it.isRecurring }
            .sortedByDescending { it.dateMillis }

        ExpensesUiState(
          selectedDay = ctrl.selectedDay,
          displayedYear = year,
          displayedMonth = month,
          isMonthExpanded = ctrl.isMonthExpanded,
          filter = ctrl.filter,
          selectedCategoryFilterId = validCatFilter,
          categories = categories,
          visibleOccurrences = visibleOccurrences,
          installmentPlans = installmentPlans,
          recurringPlans = recurringPlans,
          paidKeys = paidKeys,
          expenseCountByDay = countMap,
          monthTotalCents = monthTotal,
          monthOneTimeCents = monthOneTime,
          monthInstallmentCents = monthInstallment,
          monthRecurringCents = monthRecurring,
          dayTotalCents = dayTotal,
          categoryBreakdown = breakdown,
          dailyTotalsInMonth = dailyTotals,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpensesUiState(),
      )

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  // ---- Tasks actions ----

  fun selectDay(day: DayKey) {
    _selectedDay.value = day
    _displayedYearMonth.value = day.year to day.monthZeroBased
    _taskFilter.value = TaskFilter.SELECTED_DAY
  }

  fun jumpToToday() {
    selectDay(DayKey.today())
  }

  fun changeMonth(delta: Int) {
    _displayedYearMonth.update { (year, month) ->
      DateTimeUtils.addMonths(year, month, delta)
    }
  }

  fun toggleCalendarExpanded() {
    _isMonthExpanded.update { !it }
  }

  fun setTaskFilter(filter: TaskFilter) {
    _taskFilter.value = filter
  }

  suspend fun getTaskById(taskId: Long): TaskWithAttachments? = taskRepo.getTaskById(taskId)

  fun saveTask(
    task: TaskEntity,
    attachments: List<TaskAttachmentEntity>,
    onSaved: (Long) -> Unit = {},
  ) {
    viewModelScope.launch {
      val savedId = taskRepo.saveTask(task, attachments)
      val scheduledTask = task.copy(id = savedId)
      if (scheduledTask.alarmEnabled && !scheduledTask.isCompleted) {
        AlarmScheduler.scheduleTask(app, scheduledTask)
      } else {
        AlarmScheduler.cancelTask(app, savedId)
      }
      _selectedDay.value = DayKey.fromMillis(task.dueAtMillis)
      _displayedYearMonth.value =
        _selectedDay.value.year to _selectedDay.value.monthZeroBased
      _statusMessage.value =
        if (task.id == 0L) app.getString(R.string.msg_task_created)
        else app.getString(R.string.msg_task_updated)
      onSaved(savedId)
    }
  }

  fun completeTask(task: TaskEntity) {
    viewModelScope.launch {
      val updated = taskRepo.completeTask(task) ?: return@launch
      if (updated.isCompleted || !updated.alarmEnabled) {
        AlarmScheduler.cancelTask(app, updated.id)
      } else {
        AlarmScheduler.scheduleTask(app, updated)
      }
      _statusMessage.value =
        when {
          task.recurrence != RecurrenceType.NONE && !task.isCompleted ->
            app.getString(R.string.msg_task_cycle_done)
          updated.isCompleted -> app.getString(R.string.msg_task_completed)
          else -> app.getString(R.string.msg_task_reopened)
        }
    }
  }

  fun rescheduleTask(taskId: Long, newDueAtMillis: Long, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      val updated = taskRepo.rescheduleTask(taskId, newDueAtMillis)
      if (updated != null) {
        if (updated.alarmEnabled) {
          AlarmScheduler.scheduleTask(app, updated)
        }
        _selectedDay.value = DayKey.fromMillis(newDueAtMillis)
        _displayedYearMonth.value =
          _selectedDay.value.year to _selectedDay.value.monthZeroBased
        _statusMessage.value = app.getString(R.string.msg_task_rescheduled)
      }
      onDone()
    }
  }

  fun deleteTask(task: TaskEntity) {
    viewModelScope.launch {
      AlarmScheduler.cancelTask(app, task.id)
      taskRepo.deleteTask(task)
      _statusMessage.value = app.getString(R.string.msg_task_deleted)
    }
  }

  // ---- Shopping & Folders actions ----

  fun selectShoppingFolder(folderId: Long?) {
    _selectedFolderId.value = folderId
  }

  fun createShoppingFolder(name: String) {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      val newId = shoppingRepo.createFolder(clean)
      _selectedFolderId.value = newId
      _statusMessage.value = app.getString(R.string.msg_folder_created)
    }
  }

  fun renameShoppingFolder(folder: ShoppingFolderEntity, newName: String) {
    val clean = newName.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      shoppingRepo.renameFolder(folder, clean)
      _statusMessage.value = app.getString(R.string.msg_folder_renamed)
    }
  }

  fun deleteShoppingFolder(folder: ShoppingFolderEntity) {
    viewModelScope.launch {
      if (_selectedFolderId.value == folder.id) {
        _selectedFolderId.value = null
      }
      shoppingRepo.deleteFolder(folder)
      _statusMessage.value = app.getString(R.string.msg_folder_deleted)
    }
  }

  fun addShoppingItem(name: String = "", quantity: Int = 1, onCreated: (Long) -> Unit = {}) {
    viewModelScope.launch {
      val folderId = shoppingUiState.value.selectedFolderId
      val id = shoppingRepo.addItem(name = name, quantity = quantity, folderId = folderId)
      onCreated(id)
    }
  }

  fun updateShoppingItem(item: ShoppingItemEntity) {
    viewModelScope.launch { shoppingRepo.updateItem(item) }
  }

  fun deleteShoppingItem(item: ShoppingItemEntity) {
    viewModelScope.launch { shoppingRepo.deleteItem(item) }
  }

  fun clearCheckedShoppingItems() {
    viewModelScope.launch {
      shoppingRepo.clearChecked(shoppingUiState.value.selectedFolderId)
      _statusMessage.value = app.getString(R.string.msg_shopping_cleared)
    }
  }

  // ---- Expenses & Categories actions ----

  fun selectExpenseDay(day: DayKey) {
    _expenseSelectedDay.value = day
    _expenseDisplayedYearMonth.value = day.year to day.monthZeroBased
    _expenseFilter.value = ExpenseFilter.SELECTED_DAY
  }

  fun jumpExpenseToday() {
    selectExpenseDay(DayKey.today())
  }

  fun changeExpenseMonth(delta: Int) {
    _expenseDisplayedYearMonth.update { (year, month) ->
      DateTimeUtils.addMonths(year, month, delta)
    }
  }

  fun toggleExpenseCalendarExpanded() {
    _expenseMonthExpanded.update { !it }
  }

  fun setExpenseFilter(filter: ExpenseFilter) {
    _expenseFilter.value = filter
  }

  fun setExpenseCategoryFilter(categoryId: Long?) {
    _expenseCategoryFilterId.value = categoryId
  }

  suspend fun getExpenseById(expenseId: Long): ExpenseEntity? =
    expenseRepo.getExpenseById(expenseId)

  fun createExpenseCategory(
    name: String,
    colorArgb: Long,
    iconKey: String,
    onCreated: (Long) -> Unit = {},
  ) {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      val id = expenseRepo.createCategory(clean, colorArgb, iconKey)
      _statusMessage.value = app.getString(R.string.msg_category_created)
      onCreated(id)
    }
  }

  fun deleteExpenseCategory(category: ExpenseCategoryEntity) {
    viewModelScope.launch {
      if (_expenseCategoryFilterId.value == category.id) {
        _expenseCategoryFilterId.value = null
      }
      expenseRepo.deleteCategory(category)
      _statusMessage.value = app.getString(R.string.msg_category_deleted)
    }
  }

  fun saveExpense(expense: ExpenseEntity, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      expenseRepo.saveExpense(expense)
      if (expense.isInstallment) {
        val startDay =
          ExpenseCalculator.installmentDayForOffset(
            expense.startYear,
            expense.startMonthZeroBased,
            expense.dayOfMonth,
            0,
          )
        _expenseSelectedDay.value = startDay
        _expenseDisplayedYearMonth.value = expense.startYear to expense.startMonthZeroBased
      } else {
        val day = DayKey.fromMillis(expense.dateMillis)
        _expenseSelectedDay.value = day
        _expenseDisplayedYearMonth.value = day.year to day.monthZeroBased
      }
      _statusMessage.value =
        if (expense.id == 0L) app.getString(R.string.msg_expense_created)
        else app.getString(R.string.msg_expense_updated)
      onSaved()
    }
  }

  fun deleteExpense(expense: ExpenseEntity) {
    viewModelScope.launch {
      expenseRepo.deleteExpense(expense)
      _statusMessage.value = app.getString(R.string.msg_expense_deleted)
    }
  }

  fun toggleOccurrencePaid(expenseId: Long, occurrenceKey: String, markAsPaid: Boolean) {
    viewModelScope.launch {
      expenseRepo.setOccurrencePaid(expenseId, occurrenceKey, markAsPaid)
      _statusMessage.value =
        app.getString(
          if (markAsPaid) R.string.msg_installment_marked_paid
          else R.string.msg_installment_marked_unpaid
        )
    }
  }

  // ---- Theme & Backup actions ----

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { themeRepo.setThemeMode(mode) }
  }

  fun setUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch { themeRepo.setUseDynamicColor(enabled) }
  }

  fun setPalette(palette: AppPalette) {
    viewModelScope.launch { themeRepo.setPalette(palette) }
  }

  fun exportBackup(targetUri: Uri) {
    viewModelScope.launch {
      val result =
        withContext(Dispatchers.IO) {
          val prefs = themeRepo.getSnapshot()
          val tasks = taskRepo.getAllTasksSnapshot()
          val folders = shoppingRepo.getAllFoldersSnapshot()
          val shopping = shoppingRepo.getAllItemsSnapshot()
          val categories = expenseRepo.getAllCategoriesSnapshot()
          val expenses = expenseRepo.getAllExpensesSnapshot()
          val payments = expenseRepo.getAllPaymentsSnapshot()
          BackupManager.exportToUri(
            context = app,
            targetUri = targetUri,
            preferences = prefs,
            tasks = tasks,
            shoppingItems = shopping,
            shoppingFolders = folders,
            expenseCategories = categories,
            expenses = expenses,
            expensePayments = payments,
          )
        }
      result.fold(
        onSuccess = { count ->
          _statusMessage.value = app.getString(R.string.msg_backup_exported, count)
        },
        onFailure = { err ->
          _statusMessage.value =
            app.getString(
              R.string.msg_backup_export_failed,
              err.localizedMessage ?: app.getString(R.string.msg_unknown_error),
            )
        },
      )
    }
  }

  fun loadBackupPreview(sourceUri: Uri) {
    viewModelScope.launch {
      val result = withContext(Dispatchers.IO) { BackupManager.parseFromUri(app, sourceUri) }
      result.fold(
        onSuccess = { preview -> _backupPreview.value = preview },
        onFailure = { err ->
          _statusMessage.value =
            app.getString(
              R.string.msg_backup_invalid,
              err.localizedMessage ?: app.getString(R.string.msg_unknown_error),
            )
        },
      )
    }
  }

  fun dismissBackupPreview() {
    _backupPreview.value = null
  }

  fun applyBackupImport(payload: BackupPayload, replaceExisting: Boolean) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        if (replaceExisting) {
          val existing = taskRepo.getPendingAlarmTasks()
          existing.forEach { AlarmScheduler.cancelTask(app, it.id) }
          val inserted = taskRepo.replaceAllTasks(payload.tasks)
          shoppingRepo.replaceAllShoppingData(payload.shoppingFolders, payload.shoppingItems)
          expenseRepo.replaceAllExpenses(
            payload.expenseCategories,
            payload.expenses,
            payload.expensePayments,
          )
          themeRepo.restorePreferences(payload.preferences)
          inserted.filter { !it.isCompleted && it.alarmEnabled }.forEach {
            AlarmScheduler.scheduleTask(app, it)
          }
        } else {
          val inserted = taskRepo.insertAdditionalTasks(payload.tasks)
          shoppingRepo.insertAdditionalShoppingData(payload.shoppingFolders, payload.shoppingItems)
          expenseRepo.insertAdditionalExpenses(
            payload.expenseCategories,
            payload.expenses,
            payload.expensePayments,
          )
          inserted.filter { !it.isCompleted && it.alarmEnabled }.forEach {
            AlarmScheduler.scheduleTask(app, it)
          }
        }
      }
      _backupPreview.value = null
      _statusMessage.value = app.getString(R.string.msg_backup_imported)
    }
  }
}
