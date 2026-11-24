package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val database: AppDatabase) {
  private val expenseDao = database.expenseDao()

  val allCategories: Flow<List<ExpenseCategoryEntity>> = expenseDao.observeAllCategories()

  val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.observeAllExpenses()

  val allPayments: Flow<List<ExpensePaymentEntity>> = expenseDao.observeAllPayments()

  suspend fun getAllCategoriesSnapshot(): List<ExpenseCategoryEntity> =
    expenseDao.getAllCategoriesSnapshot()

  suspend fun getAllExpensesSnapshot(): List<ExpenseEntity> =
    expenseDao.getAllExpensesSnapshot()

  suspend fun getAllPaymentsSnapshot(): List<ExpensePaymentEntity> =
    expenseDao.getAllPaymentsSnapshot()

  suspend fun getExpenseById(id: Long): ExpenseEntity? = expenseDao.getExpenseById(id)

  suspend fun createCategory(name: String, colorArgb: Long, iconKey: String): Long {
    val clean = name.trim()
    val existing = expenseDao.findCategoryByNameIgnoreCase(clean)
    if (existing != null) {
      return existing.id
    }
    return expenseDao.insertCategory(
      ExpenseCategoryEntity(
        name = clean,
        colorArgb = colorArgb,
        iconKey = iconKey,
      )
    )
  }

  suspend fun deleteCategory(category: ExpenseCategoryEntity) =
    expenseDao.deleteCategory(category)

  suspend fun saveExpense(expense: ExpenseEntity): Long {
    return if (expense.id == 0L) {
      expenseDao.insertExpense(expense)
    } else {
      expenseDao.updateExpense(expense)
      expense.id
    }
  }

  suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

  suspend fun setOccurrencePaid(expenseId: Long, occurrenceKey: String, paid: Boolean) {
    if (paid) {
      expenseDao.insertPayment(
        ExpensePaymentEntity(expenseId = expenseId, occurrenceKey = occurrenceKey)
      )
    } else {
      expenseDao.deletePayment(expenseId = expenseId, occurrenceKey = occurrenceKey)
    }
  }

  suspend fun replaceAllExpenses(
    categories: List<ExpenseCategoryEntity>,
    expenses: List<ExpenseEntity>,
    payments: List<ExpensePaymentEntity> = emptyList(),
  ) =
    database.withTransaction {
      expenseDao.deleteAllPayments()
      expenseDao.deleteAllExpenses()
      expenseDao.deleteAllCategories()
      insertCategoriesExpensesAndPayments(categories, expenses, payments)
    }

  suspend fun insertAdditionalExpenses(
    categories: List<ExpenseCategoryEntity>,
    expenses: List<ExpenseEntity>,
    payments: List<ExpensePaymentEntity> = emptyList(),
  ) =
    database.withTransaction {
      insertCategoriesExpensesAndPayments(categories, expenses, payments)
    }

  private suspend fun insertCategoriesExpensesAndPayments(
    categories: List<ExpenseCategoryEntity>,
    expenses: List<ExpenseEntity>,
    payments: List<ExpensePaymentEntity>,
  ) {
    val categoryIdMap = mutableMapOf<Long, Long>()
    for (cat in categories) {
      val clean = cat.name.trim()
      val existing = if (clean.isNotEmpty()) expenseDao.findCategoryByNameIgnoreCase(clean) else null
      val newId = existing?.id ?: expenseDao.insertCategory(cat.copy(id = 0L, name = clean))
      if (cat.id > 0L) {
        categoryIdMap[cat.id] = newId
      }
    }
    val expenseIdMap = mutableMapOf<Long, Long>()
    for (exp in expenses) {
      val mappedCatId = exp.categoryId?.let { oldId -> categoryIdMap[oldId] }
      val newId = expenseDao.insertExpense(exp.copy(id = 0L, categoryId = mappedCatId))
      if (exp.id > 0L) {
        expenseIdMap[exp.id] = newId
      }
    }
    for (payment in payments) {
      val mappedExpenseId = expenseIdMap[payment.expenseId] ?: continue
      expenseDao.insertPayment(payment.copy(expenseId = mappedExpenseId))
    }
  }
}
