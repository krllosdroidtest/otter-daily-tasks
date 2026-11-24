package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val colorArgb: Long = 0xFF006D77,
  val iconKey: String = "category",
  val createdAtMillis: Long = System.currentTimeMillis(),
)

enum class ExpensePaymentMethod {
  DEBIT_ONE_TIME,
  CREDIT_ONE_TIME,
  CREDIT_INSTALLMENT,
  RECURRING,
}

@Entity(
  tableName = "expenses",
  foreignKeys =
    [
      ForeignKey(
        entity = ExpenseCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
      )
    ],
  indices = [Index(value = ["categoryId"])],
)
data class ExpenseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val notes: String = "",
  val totalAmountCents: Long,
  val categoryId: Long? = null,
  val isInstallment: Boolean = false,
  val installmentCount: Int = 1,
  val startYear: Int,
  val startMonthZeroBased: Int,
  val dayOfMonth: Int,
  val dateMillis: Long,
  val recurrence: RecurrenceType = RecurrenceType.NONE,
  val paymentMethod: ExpensePaymentMethod = ExpensePaymentMethod.DEBIT_ONE_TIME,
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  val isRecurring: Boolean
    get() = !isInstallment && recurrence != RecurrenceType.NONE

  fun effectivePaymentMethod(): ExpensePaymentMethod =
    when {
      isInstallment -> ExpensePaymentMethod.CREDIT_INSTALLMENT
      recurrence != RecurrenceType.NONE -> ExpensePaymentMethod.RECURRING
      paymentMethod == ExpensePaymentMethod.CREDIT_ONE_TIME -> ExpensePaymentMethod.CREDIT_ONE_TIME
      else -> ExpensePaymentMethod.DEBIT_ONE_TIME
    }
}

@Entity(
  tableName = "expense_payments",
  primaryKeys = ["expenseId", "occurrenceKey"],
  foreignKeys =
    [
      ForeignKey(
        entity = ExpenseEntity::class,
        parentColumns = ["id"],
        childColumns = ["expenseId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["expenseId"])],
)
data class ExpensePaymentEntity(
  val expenseId: Long,
  val occurrenceKey: String,
  val paidAtMillis: Long = System.currentTimeMillis(),
)

data class CategoryColorOption(val argb: Long) {
  val color: Color
    get() = Color(argb.toInt())
}

data class CategoryIconOption(val key: String, val icon: ImageVector)

object ExpenseCategoryStyleCatalog {
  val colors: List<CategoryColorOption> =
    listOf(
      CategoryColorOption(0xFF006D77), // Teal
      CategoryColorOption(0xFF2D6A4F), // Emerald
      CategoryColorOption(0xFF3F51B5), // Indigo
      CategoryColorOption(0xFFC8553D), // Terracotta
      CategoryColorOption(0xFFD97706), // Amber
      CategoryColorOption(0xFF8E24AA), // Plum
      CategoryColorOption(0xFF0284C7), // Sky
      CategoryColorOption(0xFFE11D48), // Rose
    )

  val icons: List<CategoryIconOption> =
    listOf(
      CategoryIconOption("category", Icons.Rounded.Category),
      CategoryIconOption("food", Icons.Rounded.Fastfood),
      CategoryIconOption("shopping", Icons.Rounded.ShoppingBag),
      CategoryIconOption("home", Icons.Rounded.Home),
      CategoryIconOption("car", Icons.Rounded.DirectionsCar),
      CategoryIconOption("bills", Icons.AutoMirrored.Rounded.ReceiptLong),
      CategoryIconOption("card", Icons.Rounded.CreditCard),
      CategoryIconOption("health", Icons.Rounded.LocalHospital),
      CategoryIconOption("study", Icons.Rounded.School),
      CategoryIconOption("leisure", Icons.Rounded.SportsEsports),
    )

  fun iconFor(key: String): ImageVector =
    icons.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Category
}
