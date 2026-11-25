package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
  @Query("SELECT * FROM expense_categories ORDER BY createdAtMillis ASC, id ASC")
  fun observeAllCategories(): Flow<List<ExpenseCategoryEntity>>

  @Query("SELECT * FROM expense_categories ORDER BY createdAtMillis ASC, id ASC")
  suspend fun getAllCategoriesSnapshot(): List<ExpenseCategoryEntity>

  @Query("SELECT * FROM expense_categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
  suspend fun findCategoryByNameIgnoreCase(name: String): ExpenseCategoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: ExpenseCategoryEntity): Long

  @Update
  suspend fun updateCategory(category: ExpenseCategoryEntity)

  @Delete
  suspend fun deleteCategory(category: ExpenseCategoryEntity)

  @Query("DELETE FROM expense_categories")
  suspend fun deleteAllCategories()

  @Query("SELECT * FROM credit_cards ORDER BY createdAtMillis ASC, id ASC")
  fun observeAllCreditCards(): Flow<List<CreditCardEntity>>

  @Query("SELECT * FROM credit_cards ORDER BY createdAtMillis ASC, id ASC")
  suspend fun getAllCreditCardsSnapshot(): List<CreditCardEntity>

  @Query("SELECT * FROM credit_cards WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
  suspend fun findCreditCardByNameIgnoreCase(name: String): CreditCardEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCreditCard(card: CreditCardEntity): Long

  @Update
  suspend fun updateCreditCard(card: CreditCardEntity)

  @Delete
  suspend fun deleteCreditCard(card: CreditCardEntity)

  @Query("DELETE FROM credit_cards")
  suspend fun deleteAllCreditCards()

  @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, createdAtMillis DESC")
  fun observeAllExpenses(): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, createdAtMillis DESC")
  suspend fun getAllExpensesSnapshot(): List<ExpenseEntity>

  @Query("SELECT * FROM expenses WHERE id = :id")
  suspend fun getExpenseById(id: Long): ExpenseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity): Long

  @Update
  suspend fun updateExpense(expense: ExpenseEntity)

  @Delete
  suspend fun deleteExpense(expense: ExpenseEntity)

  @Query("DELETE FROM expenses")
  suspend fun deleteAllExpenses()

  @Query("SELECT * FROM expense_payments")
  fun observeAllPayments(): Flow<List<ExpensePaymentEntity>>

  @Query("SELECT * FROM expense_payments")
  suspend fun getAllPaymentsSnapshot(): List<ExpensePaymentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayment(payment: ExpensePaymentEntity)

  @Query("DELETE FROM expense_payments WHERE expenseId = :expenseId AND occurrenceKey = :occurrenceKey")
  suspend fun deletePayment(expenseId: Long, occurrenceKey: String)

  @Query("DELETE FROM expense_payments")
  suspend fun deleteAllPayments()
}
