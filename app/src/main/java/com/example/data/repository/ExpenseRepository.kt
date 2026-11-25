package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val database: AppDatabase) {
  private val expenseDao = database.expenseDao()

  val allCategories: Flow<List<ExpenseCategoryEntity>> = expenseDao.observeAllCategories()

  val allCreditCards: Flow<List<CreditCardEntity>> = expenseDao.observeAllCreditCards()

  val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.observeAllExpenses()

  val allPayments: Flow<List<ExpensePaymentEntity>> = expenseDao.observeAllPayments()

  suspend fun getAllCategoriesSnapshot(): List<ExpenseCategoryEntity> =
    expenseDao.getAllCategoriesSnapshot()

  suspend fun getAllCreditCardsSnapshot(): List<CreditCardEntity> =
    expenseDao.getAllCreditCardsSnapshot()

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

  suspend fun saveCreditCard(card: CreditCardEntity): Long {
    val clean = card.name.trim()
    val sanitized =
      card.copy(
        name = clean,
        closingDay = card.closingDay.coerceIn(1, 31),
        dueDay = card.dueDay.coerceIn(1, 31),
      )
    return if (sanitized.id == 0L) {
      val existing = expenseDao.findCreditCardByNameIgnoreCase(clean)
      existing?.id ?: expenseDao.insertCreditCard(sanitized)
    } else {
      expenseDao.updateCreditCard(sanitized)
      sanitized.id
    }
  }

  suspend fun deleteCreditCard(card: CreditCardEntity) =
    expenseDao.deleteCreditCard(card)

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

  suspend fun setOccurrencesPaid(items: List<Pair<Long, String>>, paid: Boolean) {
    if (items.isEmpty()) return
    database.withTransaction {
      for ((expenseId, occurrenceKey) in items) {
        if (paid) {
          expenseDao.insertPayment(
            ExpensePaymentEntity(expenseId = expenseId, occurrenceKey = occurrenceKey)
          )
        } else {
          expenseDao.deletePayment(expenseId = expenseId, occurrenceKey = occurrenceKey)
        }
      }
    }
  }

  suspend fun replaceAllExpenses(
    categories: List<ExpenseCategoryEntity>,
    expenses: List<ExpenseEntity>,
    payments: List<ExpensePaymentEntity> = emptyList(),
    creditCards: List<CreditCardEntity> = emptyList(),
  ) =
    database.withTransaction {
      expenseDao.deleteAllPayments()
      expenseDao.deleteAllExpenses()
      expenseDao.deleteAllCategories()
      expenseDao.deleteAllCreditCards()
      insertCategoriesCardsExpensesAndPayments(categories, creditCards, expenses, payments)
    }

  suspend fun insertAdditionalExpenses(
    categories: List<ExpenseCategoryEntity>,
    expenses: List<ExpenseEntity>,
    payments: List<ExpensePaymentEntity> = emptyList(),
    creditCards: List<CreditCardEntity> = emptyList(),
  ) =
    database.withTransaction {
      insertCategoriesCardsExpensesAndPayments(categories, creditCards, expenses, payments)
    }

  private suspend fun insertCategoriesCardsExpensesAndPayments(
    categories: List<ExpenseCategoryEntity>,
    creditCards: List<CreditCardEntity>,
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

    val creditCardIdMap = mutableMapOf<Long, Long>()
    for (card in creditCards) {
      val clean = card.name.trim()
      val existing = if (clean.isNotEmpty()) expenseDao.findCreditCardByNameIgnoreCase(clean) else null
      val newId =
        existing?.id
          ?: expenseDao.insertCreditCard(
            card.copy(
              id = 0L,
              name = clean,
              closingDay = card.closingDay.coerceIn(1, 31),
              dueDay = card.dueDay.coerceIn(1, 31),
            )
          )
      if (card.id > 0L) {
        creditCardIdMap[card.id] = newId
      }
    }

    val expenseIdMap = mutableMapOf<Long, Long>()
    for (exp in expenses) {
      val mappedCatId = exp.categoryId?.let { oldId -> categoryIdMap[oldId] }
      val mappedCardId = exp.creditCardId?.let { oldId -> creditCardIdMap[oldId] }
      val newId =
        expenseDao.insertExpense(
          exp.copy(id = 0L, categoryId = mappedCatId, creditCardId = mappedCardId)
        )
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
