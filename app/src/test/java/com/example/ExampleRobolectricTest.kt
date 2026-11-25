package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.AlarmSoundMode
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentMethod
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
  fun `app name and topbar titles match otter format`() {
    assertEquals("otter", context.getString(R.string.app_name))
    assertEquals("otter • Shopping List", context.getString(R.string.topbar_shopping))
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

    expenseRepo.saveExpense(
      ExpenseEntity(
        title = "Supermercado",
        totalAmountCents = 35_000L,
        categoryId = catId,
        isInstallment = false,
        installmentCount = 1,
        startYear = 2026,
        startMonthZeroBased = 4,
        dayOfMonth = 12,
        dateMillis = DayKey(2026, 4, 12).toCalendarAt(12, 0).timeInMillis,
        paymentMethod = ExpensePaymentMethod.DEBIT_ONE_TIME,
      )
    )

    val creditOneTimeId =
      expenseRepo.saveExpense(
        ExpenseEntity(
          title = "Farmácia",
          totalAmountCents = 8_500L,
          categoryId = catId,
          isInstallment = false,
          installmentCount = 1,
          startYear = 2026,
          startMonthZeroBased = 4,
          dayOfMonth = 14,
          dateMillis = DayKey(2026, 4, 14).toCalendarAt(12, 0).timeInMillis,
          paymentMethod = ExpensePaymentMethod.CREDIT_ONE_TIME,
        )
      )
    expenseRepo.setOccurrencePaid(creditOneTimeId, ExpenseCalculator.singleOccurrenceKey(), true)

    val backupFile = File(context.cacheDir, "test_backup_v4.json")
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
    assertEquals(4, preview.expenseCount)
    assertEquals(2, preview.payload.expensePayments.size)
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
    assertEquals(4, restoredExps.size)
    assertEquals(2, restoredPayments.size)

    val restoredDebit = restoredExps.first { it.title == "Supermercado" }
    assertEquals(ExpensePaymentMethod.DEBIT_ONE_TIME, restoredDebit.effectivePaymentMethod())
    val restoredCreditSingle = restoredExps.first { it.title == "Farmácia" }
    assertEquals(ExpensePaymentMethod.CREDIT_ONE_TIME, restoredCreditSingle.effectivePaymentMethod())
    val restoredInst = restoredExps.first { it.isInstallment }
    assertEquals(ExpensePaymentMethod.CREDIT_INSTALLMENT, restoredInst.effectivePaymentMethod())
    val restoredRec = restoredExps.first { it.isRecurring }
    assertEquals(ExpensePaymentMethod.RECURRING, restoredRec.effectivePaymentMethod())
    assertEquals(RecurrenceType.MONTHLY, restoredRec.recurrence)

    val mayOccurrences =
      ExpenseCalculator.occurrencesInMonth(
        restoredExps,
        2026,
        4,
        restoredPayments.map { it.expenseId to it.occurrenceKey }.toSet(),
      )
    assertEquals(
      35_000L,
      mayOccurrences.filter { it.paymentMethod == ExpensePaymentMethod.DEBIT_ONE_TIME }.sumOf { it.amountCents },
    )
    assertEquals(
      8_500L,
      mayOccurrences.filter { it.paymentMethod == ExpensePaymentMethod.CREDIT_ONE_TIME }.sumOf { it.amountCents },
    )
    assertEquals(
      40_000L,
      mayOccurrences.filter { it.paymentMethod == ExpensePaymentMethod.CREDIT_INSTALLMENT }.sumOf { it.amountCents },
    )
    assertEquals(
      12_000L,
      mayOccurrences.filter { it.paymentMethod == ExpensePaymentMethod.RECURRING }.sumOf { it.amountCents },
    )
  }

  @Test
  fun `credit card billing cycle calculation handles closing day due day and february clamping`() {
    // Card closes day 25, due day 5 (due in following month)
    val onClosing =
      ExpenseCalculator.suggestCreditBillingMonth(
        purchaseDay = DayKey(2026, 10, 25), // Nov 25
        closingDay = 25,
        dueDay = 5,
      )
    assertEquals(2026, onClosing.billingYear)
    assertEquals(11, onClosing.billingMonthZeroBased) // Dec 2026
    assertEquals(5, onClosing.dueDayOfMonth)

    val afterClosing =
      ExpenseCalculator.suggestCreditBillingMonth(
        purchaseDay = DayKey(2026, 10, 26), // Nov 26
        closingDay = 25,
        dueDay = 5,
      )
    assertEquals(2027, afterClosing.billingYear)
    assertEquals(0, afterClosing.billingMonthZeroBased) // Jan 2027

    // Card closes day 10, due day 20 (due in same month as closing)
    val sameMonthDue =
      ExpenseCalculator.suggestCreditBillingMonth(
        purchaseDay = DayKey(2026, 10, 8), // Nov 8
        closingDay = 10,
        dueDay = 20,
      )
    assertEquals(2026, sameMonthDue.billingYear)
    assertEquals(10, sameMonthDue.billingMonthZeroBased) // Nov 2026
    assertEquals(20, sameMonthDue.dueDayOfMonth)

    // Feb 2026 clamping for closingDay = 31 -> effectiveClosing = 28
    val febClamp =
      ExpenseCalculator.suggestCreditBillingMonth(
        purchaseDay = DayKey(2026, 1, 28), // Feb 28
        closingDay = 31,
        dueDay = 5,
      )
    assertEquals(2026, febClamp.billingYear)
    assertEquals(2, febClamp.billingMonthZeroBased) // Mar 2026
  }

  @Test
  fun `credit purchases stay in purchase month totals and group separately into next month invoices`() =
    runTest {
      val nubankId =
        expenseRepo.saveCreditCard(
          CreditCardEntity(name = "Nubank", colorArgb = 0xFF8E24AA, closingDay = 25, dueDay = 5)
        )
      // Deduplicate case-insensitively on insert
      val dupId =
        expenseRepo.saveCreditCard(
          CreditCardEntity(name = " nubank ", colorArgb = 0xFF006D77, closingDay = 10, dueDay = 20)
        )
      assertEquals(nubankId, dupId)

      val interId =
        expenseRepo.saveCreditCard(
          CreditCardEntity(name = "Inter", colorArgb = 0xFFD97706, closingDay = 20, dueDay = 2)
        )

      // Nov 15 purchase on Nubank (closes 25, due 5) -> enters Dec 2026 bill
      val novPurchaseDay = DayKey(2026, 10, 15)
      val novBilling = ExpenseCalculator.suggestCreditBillingMonth(novPurchaseDay, 25, 5)
      val novCreditId =
        expenseRepo.saveExpense(
          ExpenseEntity(
            title = "Supermercado Crédito",
            totalAmountCents = 25_000L,
            creditCardId = nubankId,
            isInstallment = false,
            installmentCount = 1,
            startYear = novBilling.billingYear,
            startMonthZeroBased = novBilling.billingMonthZeroBased,
            dayOfMonth = novBilling.dueDayOfMonth,
            dateMillis = novPurchaseDay.toCalendarAt(12, 0).timeInMillis,
            paymentMethod = ExpensePaymentMethod.CREDIT_ONE_TIME,
          )
        )

      // Dec 10 debit purchase -> only in Dec Compras do mês, never in invoices
      expenseRepo.saveExpense(
        ExpenseEntity(
          title = "Padaria Débito",
          totalAmountCents = 3_500L,
          isInstallment = false,
          installmentCount = 1,
          startYear = 2026,
          startMonthZeroBased = 11,
          dayOfMonth = 10,
          dateMillis = DayKey(2026, 11, 10).toCalendarAt(12, 0).timeInMillis,
          paymentMethod = ExpensePaymentMethod.DEBIT_ONE_TIME,
        )
      )

      // 2x installment on Inter starting Dec 2026
      val instId =
        expenseRepo.saveExpense(
          ExpenseEntity(
            title = "Tênis",
            totalAmountCents = 40_000L,
            creditCardId = interId,
            isInstallment = true,
            installmentCount = 2,
            startYear = 2026,
            startMonthZeroBased = 11, // Dec 2026
            dayOfMonth = 2,
            dateMillis = DayKey(2026, 10, 10).toCalendarAt(12, 0).timeInMillis,
            paymentMethod = ExpensePaymentMethod.CREDIT_INSTALLMENT,
          )
        )

      val cards = expenseRepo.allCreditCards.first()
      val expenses = expenseRepo.allExpenses.first()
      var paidKeys = expenseRepo.allPayments.first().map { it.expenseId to it.occurrenceKey }.toSet()

      // Nov Compras do mês MUST contain the Nov credit one-time purchase (25_000)
      val novPurchases = ExpenseCalculator.occurrencesInMonth(expenses, 2026, 10, paidKeys)
      assertEquals(1, novPurchases.size)
      assertEquals(25_000L, novPurchases.sumOf { it.amountCents })

      // Dec Compras do mês MUST contain only Dec debit (3_500) + Dec installment (20_000) = 23_500 (NOT Nov's credit one-time!)
      val decPurchases = ExpenseCalculator.occurrencesInMonth(expenses, 2026, 11, paidKeys)
      assertEquals(23_500L, decPurchases.sumOf { it.amountCents })
      assertFalse(decPurchases.any { it.expense.id == novCreditId })

      // Dec Faturas a pagar no mês MUST contain Inter (20_000) due Dec 2 and Nubank (25_000) due Dec 5
      val decInvoices = ExpenseCalculator.invoicesInMonth(expenses, cards, 2026, 11, paidKeys)
      assertEquals(2, decInvoices.size)
      assertEquals(45_000L, decInvoices.sumOf { it.totalCents })

      val interInvoice = decInvoices.first { it.card?.id == interId }
      assertEquals(20_000L, interInvoice.totalCents)
      assertEquals(DayKey(2026, 11, 2), interInvoice.dueDayKey)

      val nubankInvoice = decInvoices.first { it.card?.id == nubankId }
      assertEquals(25_000L, nubankInvoice.totalCents)
      assertEquals(DayKey(2026, 11, 5), nubankInvoice.dueDayKey)

      // Pay whole Nubank invoice
      expenseRepo.setOccurrencesPaid(
        nubankInvoice.occurrences.map { it.expense.id to it.occurrenceKey },
        true,
      )
      paidKeys = expenseRepo.allPayments.first().map { it.expenseId to it.occurrenceKey }.toSet()
      val updatedNubankInvoice =
        ExpenseCalculator.invoicesInMonth(expenses, cards, 2026, 11, paidKeys).first {
          it.card?.id == nubankId
        }
      assertTrue(updatedNubankInvoice.allPaid)
      assertEquals(25_000L, updatedNubankInvoice.paidCents)

      // Backup v5 roundtrip preserves credit cards and associations
      val backupFile = File(context.cacheDir, "test_backup_v5.json")
      val uri = Uri.fromFile(backupFile)
      val exportRes =
        BackupManager.exportToUri(
          context = context,
          targetUri = uri,
          preferences = ThemePreferences(),
          tasks = emptyList(),
          shoppingItems = emptyList(),
          shoppingFolders = emptyList(),
          expenseCategories = emptyList(),
          creditCards = cards,
          expenses = expenses,
          expensePayments = expenseRepo.allPayments.first(),
        )
      assertTrue(exportRes.isSuccess)
      val preview = BackupManager.parseFromUri(context, uri).getOrThrow()
      assertEquals(2, preview.creditCardCount)

      expenseRepo.replaceAllExpenses(
        preview.payload.expenseCategories,
        preview.payload.expenses,
        preview.payload.expensePayments,
        preview.payload.creditCards,
      )
      val restoredCards = expenseRepo.allCreditCards.first()
      val restoredExpenses = expenseRepo.allExpenses.first()
      val restoredCredit = restoredExpenses.first { it.title == "Supermercado Crédito" }
      assertEquals(2, restoredCards.size)
      assertEquals(restoredCards.first { it.name == "Nubank" }.id, restoredCredit.creditCardId)

      // Deleting a card sets creditCardId = null via SET_NULL without deleting expenses
      val cardToDelete = restoredCards.first { it.name == "Inter" }
      expenseRepo.deleteCreditCard(cardToDelete)
      val afterCardDelete = expenseRepo.allExpenses.first().first { it.title == "Tênis" }
      assertNull(afterCardDelete.creditCardId)
      assertTrue(instId > 0L)
    }
}
