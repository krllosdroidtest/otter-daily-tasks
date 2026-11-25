package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseCategoryStyleCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentMethod
import com.example.ui.components.CalendarPanel
import com.example.ui.components.CreateExpenseCategoryDialog
import com.example.ui.components.CreditCardDialog
import com.example.ui.components.ExpenseChartCard
import com.example.ui.components.MonthlyInvoicesCard
import com.example.ui.viewmodel.ExpenseFilter
import com.example.ui.viewmodel.ExpensesUiState
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import com.example.util.ExpenseCalculator
import com.example.util.ExpenseOccurrence
import com.example.util.InvoiceCardSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
  state: ExpensesUiState,
  onSelectDay: (DayKey) -> Unit,
  onChangeMonth: (Int) -> Unit,
  onToggleCalendar: () -> Unit,
  onJumpToday: () -> Unit,
  onFilterChange: (ExpenseFilter) -> Unit,
  onCategoryFilterChange: (Long?) -> Unit,
  onCreditCardFilterChange: (Long?) -> Unit,
  onCreateCategory: (name: String, colorArgb: Long, iconKey: String) -> Unit,
  onDeleteCategory: (ExpenseCategoryEntity) -> Unit,
  onSaveCreditCard: (CreditCardEntity) -> Unit,
  onDeleteCreditCard: (CreditCardEntity) -> Unit,
  onTogglePaid: (expenseId: Long, occurrenceKey: String, markAsPaid: Boolean) -> Unit,
  onToggleInvoicePaid: (InvoiceCardSummary, Boolean) -> Unit,
  onEditExpense: (Long) -> Unit,
  onDeleteExpense: (ExpenseEntity) -> Unit,
  onCreateExpense: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateCategoryDialog by remember { mutableStateOf(false) }
  var categoryPendingDelete by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }
  var showCreditCardDialog by remember { mutableStateOf(false) }
  var editingCreditCard by remember { mutableStateOf<CreditCardEntity?>(null) }
  var cardPendingDelete by remember { mutableStateOf<CreditCardEntity?>(null) }
  var expensePendingDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

  val categoryById = remember(state.categories) { state.categories.associateBy { it.id } }
  val cardById = remember(state.creditCards) { state.creditCards.associateBy { it.id } }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "expense_calendar_panel") {
        CalendarPanel(
          selectedDay = state.selectedDay,
          displayedYear = state.displayedYear,
          displayedMonth = state.displayedMonth,
          isExpanded = state.isMonthExpanded,
          taskCountByDay = state.expenseCountByDay,
          onSelectDay = onSelectDay,
          onChangeMonth = onChangeMonth,
          onToggleExpanded = onToggleCalendar,
          onJumpToday = onJumpToday,
        )
      }

      item(key = "expense_chart_card") {
        ExpenseChartCard(
          displayedYear = state.displayedYear,
          displayedMonth = state.displayedMonth,
          monthTotalCents = state.monthTotalCents,
          monthDebitOneTimeCents = state.monthDebitOneTimeCents,
          monthCreditOneTimeCents = state.monthCreditOneTimeCents,
          monthInstallmentCents = state.monthInstallmentCents,
          monthRecurringCents = state.monthRecurringCents,
          categoryBreakdown = state.categoryBreakdown,
          dailyTotalsInMonth = state.dailyTotalsInMonth,
        )
      }

      item(key = "monthly_invoices_card") {
        MonthlyInvoicesCard(
          displayedYear = state.displayedYear,
          displayedMonth = state.displayedMonth,
          invoices = state.monthlyInvoices,
          totalInvoicesCents = state.monthInvoicesTotalCents,
          paidInvoicesCents = state.monthInvoicesPaidCents,
          onToggleInvoicePaid = onToggleInvoicePaid,
          onToggleOccurrencePaid = onTogglePaid,
          onEditExpense = onEditExpense,
        )
      }

      item(key = "expense_filters_and_categories") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ExpenseFilter.entries.forEach { filter ->
              FilterChip(
                selected = state.filter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(filter.labelRes)) },
                modifier = Modifier.testTag("expense_filter_${filter.name.lowercase()}"),
              )
            }
          }

          // Credit card filter & management chips
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = state.selectedCreditCardFilterId == null,
              onClick = { onCreditCardFilterChange(null) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.CreditCard,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(stringResource(R.string.credit_card_all_filter)) },
              modifier = Modifier.testTag("expense_card_all"),
            )

            state.creditCards.forEach { card ->
              val isSelected = state.selectedCreditCardFilterId == card.id
              val cardColor = Color(card.colorArgb.toInt())
              FilterChip(
                selected = isSelected,
                onClick = { onCreditCardFilterChange(if (isSelected) null else card.id) },
                leadingIcon = {
                  Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(cardColor)
                  )
                },
                trailingIcon =
                  if (isSelected) {
                    {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Edit,
                          contentDescription = stringResource(R.string.credit_card_edit_cd),
                          modifier =
                            Modifier.size(16.dp)
                              .clickable {
                                editingCreditCard = card
                                showCreditCardDialog = true
                              }
                              .testTag("edit_card_${card.id}"),
                        )
                        Icon(
                          imageVector = Icons.Rounded.Close,
                          contentDescription = stringResource(R.string.credit_card_delete_cd),
                          modifier =
                            Modifier.size(16.dp)
                              .clickable { cardPendingDelete = card }
                              .testTag("delete_card_${card.id}"),
                        )
                      }
                    }
                  } else {
                    null
                  },
                label = { Text(card.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("expense_card_chip_${card.id}"),
              )
            }

            AssistChip(
              onClick = {
                editingCreditCard = null
                showCreditCardDialog = true
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
              label = { Text(stringResource(R.string.credit_card_new)) },
              modifier = Modifier.testTag("expense_new_card_button"),
            )
          }

          // All created categories appear visibly in FlowRow so none are hidden off-screen
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = state.selectedCategoryFilterId == null,
              onClick = { onCategoryFilterChange(null) },
              label = { Text(stringResource(R.string.expense_category_all_filter)) },
              modifier = Modifier.testTag("expense_category_all"),
            )

            state.categories.forEach { cat ->
              val isSelected = state.selectedCategoryFilterId == cat.id
              val catColor = Color(cat.colorArgb.toInt())
              FilterChip(
                selected = isSelected,
                onClick = { onCategoryFilterChange(if (isSelected) null else cat.id) },
                leadingIcon = {
                  Icon(
                    imageVector = ExpenseCategoryStyleCatalog.iconFor(cat.iconKey),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(18.dp),
                  )
                },
                trailingIcon =
                  if (isSelected) {
                    {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.expense_category_delete_cd),
                        modifier =
                          Modifier.size(16.dp)
                            .clickable { categoryPendingDelete = cat }
                            .testTag("delete_category_${cat.id}"),
                      )
                    }
                  } else {
                    null
                  },
                label = { Text(cat.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("expense_category_chip_${cat.id}"),
              )
            }

            AssistChip(
              onClick = { showCreateCategoryDialog = true },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
              label = { Text(stringResource(R.string.expense_category_new)) },
              modifier = Modifier.testTag("expense_new_category_button"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val headerTitle =
              when (state.filter) {
                ExpenseFilter.SELECTED_DAY -> DateTimeUtils.formatDayHeader(state.selectedDay)
                ExpenseFilter.MONTH ->
                  stringResource(
                    R.string.expense_list_month_header,
                    DateTimeUtils.formatMonthYear(state.displayedYear, state.displayedMonth),
                  )
                ExpenseFilter.INSTALLMENTS -> stringResource(R.string.expense_list_installments_header)
                ExpenseFilter.RECURRING -> stringResource(R.string.expense_list_recurring_header)
              }
            Text(
              text = headerTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              val badgeText =
                if (state.filter == ExpenseFilter.SELECTED_DAY) {
                  stringResource(
                    R.string.expense_day_total,
                    ExpenseCalculator.formatCurrency(state.dayTotalCents),
                  )
                } else {
                  val count =
                    when (state.filter) {
                      ExpenseFilter.INSTALLMENTS -> state.installmentPlans.size
                      ExpenseFilter.RECURRING -> state.recurringPlans.size
                      else -> state.visibleOccurrences.size
                    }
                  stringResource(R.string.expense_count_badge, count)
                }
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      when (state.filter) {
        ExpenseFilter.INSTALLMENTS -> {
          if (state.installmentPlans.isEmpty()) {
            item(key = "empty_installments") {
              EmptyExpensesCard(filter = state.filter, onCreateExpense = onCreateExpense)
            }
          } else {
            items(items = state.installmentPlans, key = { "plan_${it.id}" }) { plan ->
              InstallmentPlanCard(
                expense = plan,
                category = plan.categoryId?.let { categoryById[it] },
                creditCard = plan.creditCardId?.let { cardById[it] },
                paidKeys = state.paidKeys,
                onTogglePaid = onTogglePaid,
                onEdit = { onEditExpense(plan.id) },
                onDelete = { expensePendingDelete = plan },
              )
            }
          }
        }
        ExpenseFilter.RECURRING -> {
          if (state.recurringPlans.isEmpty()) {
            item(key = "empty_recurring") {
              EmptyExpensesCard(filter = state.filter, onCreateExpense = onCreateExpense)
            }
          } else {
            items(items = state.recurringPlans, key = { "rec_plan_${it.id}" }) { plan ->
              RecurringPlanCard(
                expense = plan,
                category = plan.categoryId?.let { categoryById[it] },
                onEdit = { onEditExpense(plan.id) },
                onDelete = { expensePendingDelete = plan },
              )
            }
          }
        }
        else -> {
          if (state.visibleOccurrences.isEmpty()) {
            item(key = "empty_expenses") {
              EmptyExpensesCard(filter = state.filter, onCreateExpense = onCreateExpense)
            }
          } else {
            items(
              items = state.visibleOccurrences,
              key = { "${it.expense.id}_${it.occurrenceKey}" },
            ) { occurrence ->
              ExpenseOccurrenceCard(
                occurrence = occurrence,
                category = occurrence.expense.categoryId?.let { categoryById[it] },
                creditCard = occurrence.expense.creditCardId?.let { cardById[it] },
                onTogglePaid = {
                  onTogglePaid(
                    occurrence.expense.id,
                    occurrence.occurrenceKey,
                    !occurrence.isPaid,
                  )
                },
                onEdit = { onEditExpense(occurrence.expense.id) },
                onDelete = { expensePendingDelete = occurrence.expense },
              )
            }
          }
        }
      }
    }
  }

  if (showCreateCategoryDialog) {
    CreateExpenseCategoryDialog(
      existingCategories = state.categories,
      onSelectExisting = { categoryId ->
        onCategoryFilterChange(categoryId)
        showCreateCategoryDialog = false
      },
      onDismiss = { showCreateCategoryDialog = false },
      onCreate = { name, colorArgb, iconKey ->
        onCreateCategory(name, colorArgb, iconKey)
        showCreateCategoryDialog = false
      },
    )
  }

  if (showCreditCardDialog) {
    CreditCardDialog(
      editingCard = editingCreditCard,
      onDismiss = {
        showCreditCardDialog = false
        editingCreditCard = null
      },
      onSave = { savedCard ->
        onSaveCreditCard(savedCard)
        showCreditCardDialog = false
        editingCreditCard = null
      },
    )
  }

  categoryPendingDelete?.let { category ->
    AlertDialog(
      onDismissRequest = { categoryPendingDelete = null },
      title = { Text(stringResource(R.string.expense_category_delete_title)) },
      text = { Text(stringResource(R.string.expense_category_delete_message, category.name)) },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCategory(category)
            categoryPendingDelete = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { categoryPendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  cardPendingDelete?.let { card ->
    AlertDialog(
      onDismissRequest = { cardPendingDelete = null },
      title = { Text(stringResource(R.string.credit_card_delete_title)) },
      text = { Text(stringResource(R.string.credit_card_delete_message, card.name)) },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCreditCard(card)
            cardPendingDelete = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { cardPendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  expensePendingDelete?.let { expense ->
    AlertDialog(
      onDismissRequest = { expensePendingDelete = null },
      title = { Text(stringResource(R.string.expense_delete_title)) },
      text = { Text(stringResource(R.string.expense_delete_message, expense.title)) },
      confirmButton = {
        Button(
          onClick = {
            onDeleteExpense(expense)
            expensePendingDelete = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { expensePendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun EmptyExpensesCard(filter: ExpenseFilter, onCreateExpense: () -> Unit) {
  ElevatedCard(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.AccountBalanceWallet,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(32.dp),
        )
      }
      Text(
        text =
          when (filter) {
            ExpenseFilter.SELECTED_DAY -> stringResource(R.string.expense_empty_day)
            ExpenseFilter.MONTH -> stringResource(R.string.expense_empty_month)
            ExpenseFilter.INSTALLMENTS -> stringResource(R.string.expense_empty_installments)
            ExpenseFilter.RECURRING -> stringResource(R.string.expense_empty_recurring)
          },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.expense_empty_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Button(onClick = onCreateExpense, modifier = Modifier.testTag("empty_create_expense_button")) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.expense_empty_cta))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseOccurrenceCard(
  occurrence: ExpenseOccurrence,
  category: ExpenseCategoryEntity?,
  creditCard: CreditCardEntity?,
  onTogglePaid: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val expense = occurrence.expense
  val catColor = category?.let { Color(it.colorArgb.toInt()) } ?: MaterialTheme.colorScheme.primary
  val catIcon =
    category?.let { ExpenseCategoryStyleCatalog.iconFor(it.iconKey) }
      ?: Icons.Rounded.AccountBalanceWallet

  ElevatedCard(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onEdit)
        .testTag("expense_card_${expense.id}_${occurrence.occurrenceKey}"),
    shape = RoundedCornerShape(22.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(44.dp).clip(CircleShape).background(catColor.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = catIcon,
            contentDescription = null,
            tint = catColor,
            modifier = Modifier.size(22.dp),
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = expense.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = category?.name ?: stringResource(R.string.expense_category_uncategorized),
              style = MaterialTheme.typography.labelMedium,
              color = catColor,
            )
            Text(
              text = "• ${DateTimeUtils.formatShortDate(occurrence.dayKey.startOfDayMillis())}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Text(
          text = ExpenseCalculator.formatCurrency(occurrence.amountCents),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color =
            if (occurrence.isPaid) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
      }

      if (expense.notes.isNotBlank()) {
        Text(
          text = expense.notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FlowRow(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          when (occurrence.paymentMethod) {
            ExpensePaymentMethod.CREDIT_INSTALLMENT -> {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                  Text(
                    text =
                      stringResource(
                        R.string.expense_installment_badge,
                        occurrence.installmentNumber,
                        occurrence.totalInstallments,
                      ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            ExpensePaymentMethod.RECURRING -> {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text =
                      stringResource(
                        R.string.expense_recurring_badge,
                        stringResource(occurrence.recurrence.labelRes),
                      ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            ExpensePaymentMethod.CREDIT_ONE_TIME -> {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = stringResource(R.string.expense_type_credit_one_time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            ExpensePaymentMethod.DEBIT_ONE_TIME -> {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Text(
                    text = stringResource(R.string.expense_type_debit_one_time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
          }

          if (creditCard != null) {
            val ccColor = Color(creditCard.colorArgb.toInt())
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = ccColor.copy(alpha = 0.14f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ccColor))
                Text(
                  text = creditCard.name,
                  style = MaterialTheme.typography.labelSmall,
                  color = ccColor,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          if (occurrence.paymentMethod == ExpensePaymentMethod.CREDIT_ONE_TIME) {
            val invoiceLabel =
              DateTimeUtils.formatMonthYearShort(expense.startYear, expense.startMonthZeroBased)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            ) {
              Text(
                text = stringResource(R.string.expense_invoice_badge, invoiceLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              )
            }
          }

          if (occurrence.paymentMethod != ExpensePaymentMethod.DEBIT_ONE_TIME) {
            FilterChip(
              selected = occurrence.isPaid,
              onClick = onTogglePaid,
              leadingIcon = {
                Icon(
                  imageVector =
                    if (occurrence.isPaid) Icons.Rounded.CheckCircle
                    else Icons.Rounded.RadioButtonUnchecked,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = {
                Text(
                  stringResource(
                    if (occurrence.isPaid) R.string.expense_paid_badge
                    else R.string.expense_mark_paid
                  )
                )
              },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                ),
              modifier =
                Modifier.testTag("toggle_paid_${expense.id}_${occurrence.occurrenceKey}"),
            )
          }
        }

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_expense_${expense.id}")) {
            Icon(
              Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.action_edit),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_expense_${expense.id}")) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.action_delete),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InstallmentPlanCard(
  expense: ExpenseEntity,
  category: ExpenseCategoryEntity?,
  creditCard: CreditCardEntity?,
  paidKeys: Set<Pair<Long, String>>,
  onTogglePaid: (expenseId: Long, occurrenceKey: String, markAsPaid: Boolean) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  var expanded by remember(expense.id) { mutableStateOf(false) }
  val schedule =
    remember(expense, paidKeys) {
      ExpenseCalculator.installmentScheduleForPlan(expense, paidKeys)
    }
  val paidCount = schedule.count { it.isPaid }

  val catColor = category?.let { Color(it.colorArgb.toInt()) } ?: MaterialTheme.colorScheme.primary
  val catIcon =
    category?.let { ExpenseCategoryStyleCatalog.iconFor(it.iconKey) } ?: Icons.Rounded.CreditCard
  val perInstallment =
    ExpenseCalculator.installmentAmountCents(expense.totalAmountCents, expense.installmentCount, 0)
  val startLabel =
    DateTimeUtils.formatMonthYearShort(expense.startYear, expense.startMonthZeroBased)

  ElevatedCard(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
    shape = RoundedCornerShape(22.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(44.dp).clip(CircleShape).background(catColor.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = catIcon,
            contentDescription = null,
            tint = catColor,
            modifier = Modifier.size(22.dp),
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = expense.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              stringResource(
                R.string.expense_installment_plan_info,
                expense.installmentCount,
                ExpenseCalculator.formatCurrency(perInstallment),
                ExpenseCalculator.formatCurrency(expense.totalAmountCents),
                startLabel,
              ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = stringResource(R.string.expense_installment_day_label, expense.dayOfMonth),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (creditCard != null) {
              val ccColor = Color(creditCard.colorArgb.toInt())
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = ccColor.copy(alpha = 0.14f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ccColor))
                  Text(
                    text = creditCard.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = ccColor,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color =
                if (paidCount == schedule.size) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text =
                  stringResource(
                    R.string.expense_installment_paid_progress,
                    paidCount,
                    schedule.size,
                  ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color =
                  if (paidCount == schedule.size) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }

        Row {
          IconButton(onClick = onEdit) {
            Icon(
              Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.action_edit),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.action_delete),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.testTag("expand_installments_${expense.id}"),
      ) {
        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
          stringResource(
            if (expanded) R.string.expense_installments_collapse
            else R.string.expense_installments_expand
          )
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          schedule.forEach { occ ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (occ.isPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceContainer,
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    onTogglePaid(expense.id, occ.occurrenceKey, !occ.isPaid)
                  }
                  .testTag("plan_installment_row_${expense.id}_${occ.installmentNumber}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector =
                      if (occ.isPaid) Icons.Rounded.CheckCircle
                      else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription =
                      stringResource(
                        if (occ.isPaid) R.string.expense_mark_unpaid
                        else R.string.expense_mark_paid
                      ),
                    tint =
                      if (occ.isPaid) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                  Column {
                    Text(
                      text =
                        stringResource(
                          R.string.expense_installment_badge,
                          occ.installmentNumber,
                          occ.totalInstallments,
                        ),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Medium,
                    )
                    Text(
                      text = DateTimeUtils.formatShortDate(occ.dayKey.startOfDayMillis()),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = ExpenseCalculator.formatCurrency(occ.amountCents),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  if (occ.isPaid) {
                    Text(
                      text = stringResource(R.string.expense_paid_badge),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RecurringPlanCard(
  expense: ExpenseEntity,
  category: ExpenseCategoryEntity?,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val catColor = category?.let { Color(it.colorArgb.toInt()) } ?: MaterialTheme.colorScheme.primary
  val catIcon =
    category?.let { ExpenseCategoryStyleCatalog.iconFor(it.iconKey) } ?: Icons.Rounded.Repeat
  val startLabel = DateTimeUtils.formatShortDate(expense.dateMillis)

  ElevatedCard(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
    shape = RoundedCornerShape(22.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(44.dp).clip(CircleShape).background(catColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = catIcon,
          contentDescription = null,
          tint = catColor,
          modifier = Modifier.size(22.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = expense.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            stringResource(
              R.string.expense_recurring_plan_info,
              stringResource(expense.recurrence.labelRes),
              ExpenseCalculator.formatCurrency(expense.totalAmountCents),
              startLabel,
            ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = category?.name ?: stringResource(R.string.expense_category_uncategorized),
          style = MaterialTheme.typography.labelSmall,
          color = catColor,
        )
      }

      Row {
        IconButton(onClick = onEdit) {
          Icon(
            Icons.Rounded.Edit,
            contentDescription = stringResource(R.string.action_edit),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDelete) {
          Icon(
            Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.action_delete),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
