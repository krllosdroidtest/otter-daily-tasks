package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.AlarmSoundMode
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.ThemePreferences
import com.example.ui.theme.AppPalette
import com.example.ui.theme.ThemeMode
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import com.example.util.ExpenseCalculator
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var taskRepo: TaskRepository
  private lateinit var shoppingRepo: ShoppingRepository
  private lateinit var expenseRepo: ExpenseRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    taskRepo = TaskRepository(db)
    shoppingRepo = ShoppingRepository(db)
    expenseRepo = ExpenseRepository(db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches otter`() {
    assertEquals("otter", context.getString(R.string.app_name))
  }

  @Test
  fun `future recurring occurrences are projected onto calendar without completing current task`() {
    val startDay = DayKey(2026, 4, 10)
    val baseDue = startDay.toCalendarAt(9, 30).timeInMillis
    val weeklyTask =
      TaskEntity(
        id = 1L,
        title = "Weekly review",
        dueAtMillis = baseDue,
        recurrence = RecurrenceType.WEEKLY,
        isCompleted = false,
      )
    val oneTimeTask =
      TaskEntity(
        id = 2L,
        title = "One time",
        dueAtMillis = baseDue,
        recurrence = RecurrenceType.NONE,
        isCompleted = false,
      )

    val rangeStart = startDay.startOfDayMillis()
    val rangeEnd = DayKey(2026, 4, 30).endOfDayMillis()

    val weeklyOccurrences = DateTimeUtils.generateOccurrencesInRange(weeklyTask, rangeStart, rangeEnd)
    assertEquals(3, weeklyOccurrences.size) // May 10, May 17, May 24 (month is 0-indexed: 4 = May)
    assertEquals(DayKey(2026, 4, 10), DayKey.fromMillis(weeklyOccurrences[0]))
    assertEquals(DayKey(2026, 4, 17), DayKey.fromMillis(weeklyOccurrences[1]))
    assertEquals(DayKey(2026, 4, 24), DayKey.fromMillis(weeklyOccurrences[2]))

    val oneTimeOccurrences = DateTimeUtils.generateOccurrencesInRange(oneTimeTask, rangeStart, rangeEnd)
    assertEquals(1, oneTimeOccurrences.size)
  }

  @Test
  fun `unlimited tasks per day and recurrence advancement`() = runTest {
    val baseDue = System.currentTimeMillis() + 3_600_000L
    // Insert 15 tasks on the same day
    for (i in 1..15) {
      taskRepo.saveTask(
        TaskEntity(
          title = "Tarefa $i",
          description = "Descrição $i",
          dueAtMillis = baseDue + i * 60_000L,
          recurrence = if (i == 1) RecurrenceType.WEEKLY else RecurrenceType.NONE,
          reminderOffsetMinutes = 10080, // 1 week before
          alarmSoundMode = AlarmSoundMode.SYSTEM,
        ),
        emptyList(),
      )
    }

    val all = taskRepo.allTasks.first()
    assertEquals(15, all.size)

    // Completing a weekly task keeps it pending and advances due date by 7 days
    val weeklyTask = all.first { it.task.recurrence == RecurrenceType.WEEKLY }.task
    val updatedWeekly = taskRepo.completeTask(weeklyTask)!!
    assertFalse(updatedWeekly.isCompleted)
    assertEquals(
      DateTimeUtils.nextOccurrenceMillis(weeklyTask.dueAtMillis, RecurrenceType.WEEKLY),
      updatedWeekly.dueAtMillis,
    )
  }

  @Test
  fun `shopping folders support creation rename and cascade delete`() = runTest {
    val folderId = shoppingRepo.createFolder("Mercado")
    shoppingRepo.addItem("Café", 2, folderId = folderId)
    shoppingRepo.addItem("Sabonete", 1, folderId = null)

    var folders = shoppingRepo.allFolders.first()
    assertEquals(1, folders.size)
    assertEquals("Mercado", folders.first().name)

    shoppingRepo.renameFolder(folders.first(), "Supermercado")
    folders = shoppingRepo.allFolders.first()
    assertEquals("Supermercado", folders.first().name)

    // Deleting the folder removes its items via CASCADE but preserves general items
    shoppingRepo.deleteFolder(folders.first())
    assertTrue(shoppingRepo.allFolders.first().isEmpty())
    val remainingItems = shoppingRepo.allItems.first()
    assertEquals(1, remainingItems.size)
    assertEquals("Sabonete", remainingItems.first().name)
    assertNull(remainingItems.first().folderId)
  }

  @Test
  fun `expense installments distribute cents clamp days and support paid toggle`() = runTest {
    val catId = expenseRepo.createCategory("Eletrônicos", 0xFF006D77, "card")
    // 3 installments of 100.00 (10000 cents) starting Jan 2026 on day 31
    val expenseId =
      expenseRepo.saveExpense(
        ExpenseEntity(
          title = "Monitor",
          totalAmountCents = 10_000L,
          categoryId = catId,
          isInstallment = true,
          installmentCount = 3,
          startYear = 2026,
          startMonthZeroBased = 0, // January
          dayOfMonth = 31,
          dateMillis = DayKey(2026, 0, 31).toCalendarAt(12, 0).timeInMillis,
        )
      )

    // Mark installment 2 (February) as paid
    expenseRepo.setOccurrencePaid(expenseId, ExpenseCalculator.installmentOccurrenceKey(2), true)

    val allExpenses = expenseRepo.allExpenses.first()
    val paidKeys = expenseRepo.allPayments.first().map { it.expenseId to it.occurrenceKey }.toSet()

    val jan = ExpenseCalculator.occurrencesInMonth(allExpenses, 2026, 0, paidKeys)
    val feb = ExpenseCalculator.occurrencesInMonth(allExpenses, 2026, 1, paidKeys)
    val mar = ExpenseCalculator.occurrencesInMonth(allExpenses, 2026, 2, paidKeys)
    val apr = ExpenseCalculator.occurrencesInMonth(allExpenses, 2026, 3, paidKeys)

    assertEquals(1, jan.size)
    assertEquals(3334L, jan.first().amountCents)
    assertEquals(DayKey(2026, 0, 31), jan.first().dayKey)
    assertEquals(1, jan.first().installmentNumber)
    assertFalse(jan.first().isPaid)

    assertEquals(1, feb.size)
    assertEquals(3333L, feb.first().amountCents)
    assertEquals(DayKey(2026, 1, 28), feb.first().dayKey) // 2026 is not leap year
    assertEquals(2, feb.first().installmentNumber)
    assertTrue(feb.first().isPaid)

    assertEquals(1, mar.size)
    assertEquals(3333L, mar.first().amountCents)
    assertEquals(DayKey(2026, 2, 31), mar.first().dayKey)
    assertEquals(3, mar.first().installmentNumber)
    assertFalse(mar.first().isPaid)

    assertTrue(apr.isEmpty())

    // Schedule helper reports 1/3 paid, then 0/3 when unmarked
    val schedule = ExpenseCalculator.installmentScheduleForPlan(allExpenses.first(), paidKeys)
    assertEquals(3, schedule.size)
    assertEquals(1, schedule.count { it.isPaid })

    expenseRepo.setOccurrencePaid(expenseId, ExpenseCalculator.installmentOccurrenceKey(2), false)
    val paidKeysAfter =
      expenseRepo.allPayments.first().map { it.expenseId to it.occurrenceKey }.toSet()
    assertTrue(paidKeysAfter.isEmpty())

    // Deleting category keeps expense with categoryId = null (SET_NULL)
    val category = expenseRepo.allCategories.first().first()
    expenseRepo.deleteCategory(category)
    val afterCatDelete = expenseRepo.allExpenses.first().first()
    assertNull(afterCatDelete.categoryId)
  }

  @Test
  fun `recurring expenses project onto future months and category history avoids duplicates`() = runTest {
    val firstId = expenseRepo.createCategory("Assinaturas", 0xFF3F51B5, "bills")
    val duplicateId = expenseRepo.createCategory("  assinaturas ", 0xFFC8553D, "card")
    assertEquals(firstId, duplicateId)
    assertEquals(1, expenseRepo.allCategories.first().size)

    // Monthly recurring subscription starting Jan 31, 2026
    expenseRepo.saveExpense(
      ExpenseEntity(
        title = "Streaming",
        totalAmountCents = 4_990L,
        categoryId = firstId,
        isInstallment = false,
        installmentCount = 1,
        startYear = 2026,
        startMonthZeroBased = 0,
        dayOfMonth = 31,
        dateMillis = DayKey(2026, 0, 31).toCalendarAt(12, 0).timeInMillis,
        recurrence = RecurrenceType.MONTHLY,
      )
    )

    // Weekly expense starting May 4, 2026 (Mondays: 4, 11, 18, 25 -> 4 occurrences in May)
    expenseRepo.saveExpense(
      ExpenseEntity(
        title = "Academia",
        totalAmountCents = 5_000L,
        categoryId = firstId,
        isInstallment = false,
        installmentCount = 1,
        startYear = 2026,
        startMonthZeroBased = 4,
        dayOfMonth = 4,
        dateMillis = DayKey(2026, 4, 4).toCalendarAt(12, 0).timeInMillis,
        recurrence = RecurrenceType.WEEKLY,
      )
    )

    val all = expenseRepo.allExpenses.first()
    // Before start date (Dec 2025): empty
    assertTrue(ExpenseCalculator.occurrencesInMonth(all, 2025, 11).isEmpty())

    // Feb 2026: Monthly clamps to day 28
    val feb = ExpenseCalculator.occurrencesInMonth(all, 2026, 1)
    assertEquals(1, feb.size)
    assertTrue(feb.first().isRecurring)
    assertEquals(DayKey(2026, 1, 28), feb.first().dayKey)

    // May 2026: 1 monthly (May 31) + 4 weekly (May 4, 11, 18, 25) = 5 occurrences
    val may = ExpenseCalculator.occurrencesInMonth(all, 2026, 4)
    assertEquals(5, may.size)
    assertEquals(DayKey(2026, 4, 31), may.last().dayKey)
  }

  @Test
  fun `offline backup v3 export and import roundtrip preserves recurring expenses and paid records`() = runTest {
    val due = System.currentTimeMillis() + 7_200_000L
    taskRepo.saveTask(
      TaskEntity(title = "Pagar boleto", description = "Banco", dueAtMillis = due),
      emptyList(),
    )
    val folderId = shoppingRepo.createFolder("Farmácia")
    shoppingRepo.addItem("Arroz integral", 2, folderId = null)
    shoppingRepo.addItem("Vitamina C", 1, folderId = folderId)

    val catId = expenseRepo.createCategory("Mercado", 0xFF2D6A4F, "shopping")
    val instExpenseId =
      expenseRepo.saveExpense(
        ExpenseEntity(
          title = "Notebook",
          totalAmountCents = 240_000L,
          categoryId = catId,
          isInstallment = true,
          installmentCount = 6,
          startYear = 2026,
          startMonthZeroBased = 4,
          dayOfMonth = 10,
          dateMillis = DayKey(2026, 4, 10).toCalendarAt(12, 0).timeInMillis,
        )
      )
    expenseRepo.setOccurrencePaid(instExpenseId, ExpenseCalculator.installmentOccurrenceKey(1), true)

    expenseRepo.saveExpense(
      ExpenseEntity(
        title = "Internet",
        totalAmountCents = 12_000L,
        categoryId = catId,
        isInstallment = false,
        installmentCount = 1,
        startYear = 2026,
        startMonthZeroBased = 4,
        dayOfMonth = 15,
        dateMillis = DayKey(2026, 4, 15).toCalendarAt(12, 0).timeInMillis,
        recurrence = RecurrenceType.MONTHLY,
      )
    )

    val backupFile = File(context.cacheDir, "test_backup_v3.json")
    val uri = Uri.fromFile(backupFile)
    val prefs = ThemePreferences(ThemeMode.DARK, useDynamicColor = false, palette = AppPalette.EMERALD_SAGE)

    val exportResult =
      BackupManager.exportToUri(
        context = context,
        targetUri = uri,
        preferences = prefs,
        tasks = taskRepo.getAllTasksSnapshot(),
        shoppingItems = shoppingRepo.getAllItemsSnapshot(),
        shoppingFolders = shoppingRepo.getAllFoldersSnapshot(),
        expenseCategories = expenseRepo.getAllCategoriesSnapshot(),
        expenses = expenseRepo.getAllExpensesSnapshot(),
        expensePayments = expenseRepo.getAllPaymentsSnapshot(),
      )
    assertTrue(exportResult.isSuccess)

    val preview = BackupManager.parseFromUri(context, uri).getOrThrow()
    assertEquals(1, preview.taskCount)
    assertEquals(2, preview.shoppingItemCount)
    assertEquals(1, preview.folderCount)
    assertEquals(2, preview.expenseCount)
    assertEquals(1, preview.payload.expensePayments.size)
    assertEquals(ThemeMode.DARK, preview.payload.preferences.themeMode)
    assertEquals(AppPalette.EMERALD_SAGE, preview.payload.preferences.palette)

    // Clear and re-import
    taskRepo.replaceAllTasks(preview.payload.tasks)
    shoppingRepo.replaceAllShoppingData(preview.payload.shoppingFolders, preview.payload.shoppingItems)
    expenseRepo.replaceAllExpenses(
      preview.payload.expenseCategories,
      preview.payload.expenses,
      preview.payload.expensePayments,
    )

    assertEquals("Pagar boleto", taskRepo.allTasks.first().first().task.title)
    val restoredFolders = shoppingRepo.allFolders.first()
    assertEquals(1, restoredFolders.size)
    assertEquals("Farmácia", restoredFolders.first().name)
    val restoredItems: List<ShoppingItemEntity> = shoppingRepo.allItems.first()
    assertEquals(2, restoredItems.size)
    val itemInFolder = restoredItems.first { it.name == "Vitamina C" }
    assertEquals(restoredFolders.first().id, itemInFolder.folderId)

    val restoredCats = expenseRepo.allCategories.first()
    val restoredExps = expenseRepo.allExpenses.first()
    val restoredPayments = expenseRepo.allPayments.first()
    assertEquals(1, restoredCats.size)
    assertEquals(2, restoredExps.size)
    assertEquals(1, restoredPayments.size)
    val restoredInst = restoredExps.first { it.isInstallment }
    assertEquals(restoredInst.id, restoredPayments.first().expenseId)
    assertEquals("inst_1", restoredPayments.first().occurrenceKey)
    val restoredRec = restoredExps.first { it.isRecurring }
    assertEquals(RecurrenceType.MONTHLY, restoredRec.recurrence)
  }
}
