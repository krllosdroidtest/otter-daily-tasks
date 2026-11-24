package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseCategoryStyleCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentMethod
import com.example.data.model.RecurrenceType
import com.example.ui.components.CreateExpenseCategoryDialog
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import com.example.util.ExpenseCalculator
import java.util.Calendar

private val QUICK_INSTALLMENTS = listOf(2, 3, 6, 10, 12)

private enum class ExpenseEditorType {
  DEBIT_ONE_TIME,
  CREDIT_ONE_TIME,
  CREDIT_INSTALLMENT,
  RECURRING,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditorScreen(
  expenseId: Long?,
  initialDay: DayKey,
  categories: List<ExpenseCategoryEntity>,
  loadExpense: suspend (Long) -> ExpenseEntity?,
  onCreateCategory: (name: String, colorArgb: Long, iconKey: String, onCreated: (Long) -> Unit) -> Unit,
  onSave: (ExpenseEntity) -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  var existingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
  var title by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
  var editorType by remember { mutableStateOf(ExpenseEditorType.DEBIT_ONE_TIME) }

  // Single & recurring start date
  var dateMillis by remember(initialDay) { mutableLongStateOf(initialDay.toCalendarAt(12, 0).timeInMillis) }

  // Recurring frequency
  var recurrence by remember { mutableStateOf(RecurrenceType.MONTHLY) }

  // Installment fields
  var installmentCountText by remember { mutableStateOf("2") }
  var startYear by remember(initialDay) { mutableIntStateOf(initialDay.year) }
  var startMonth by remember(initialDay) { mutableIntStateOf(initialDay.monthZeroBased) }
  var dayOfMonth by remember(initialDay) { mutableIntStateOf(initialDay.dayOfMonth.coerceIn(1, 31)) }

  var titleError by remember { mutableStateOf(false) }
  var amountError by remember { mutableStateOf(false) }
  var installmentCountError by remember { mutableStateOf(false) }

  var showDatePicker by remember { mutableStateOf(false) }
  var showCreateCategoryDialog by remember { mutableStateOf(false) }

  LaunchedEffect(expenseId) {
    if (expenseId != null && expenseId > 0L) {
      val loaded = loadExpense(expenseId)
      if (loaded != null) {
        existingExpense = loaded
        title = loaded.title
        amountText = ExpenseCalculator.centsToEditableString(loaded.totalAmountCents)
        notes = loaded.notes
        selectedCategoryId = loaded.categoryId
        editorType =
          when (loaded.effectivePaymentMethod()) {
            ExpensePaymentMethod.CREDIT_INSTALLMENT -> ExpenseEditorType.CREDIT_INSTALLMENT
            ExpensePaymentMethod.RECURRING -> ExpenseEditorType.RECURRING
            ExpensePaymentMethod.CREDIT_ONE_TIME -> ExpenseEditorType.CREDIT_ONE_TIME
            ExpensePaymentMethod.DEBIT_ONE_TIME -> ExpenseEditorType.DEBIT_ONE_TIME
          }
        recurrence =
          if (loaded.recurrence != RecurrenceType.NONE) loaded.recurrence
          else RecurrenceType.MONTHLY
        dateMillis = loaded.dateMillis
        installmentCountText = loaded.installmentCount.coerceAtLeast(2).toString()
        startYear = loaded.startYear
        startMonth = loaded.startMonthZeroBased
        dayOfMonth = loaded.dayOfMonth.coerceIn(1, 31)
      }
    }
  }

  fun performSave() {
    val cleanTitle = title.trim()
    val amountCents = ExpenseCalculator.parseCurrencyToCents(amountText)
    val isInstallment = editorType == ExpenseEditorType.CREDIT_INSTALLMENT
    val isRecurring = editorType == ExpenseEditorType.RECURRING
    val parsedInstallments = installmentCountText.trim().toIntOrNull()

    titleError = cleanTitle.isEmpty()
    amountError = amountCents == null || amountCents <= 0L
    installmentCountError = isInstallment && (parsedInstallments == null || parsedInstallments !in 2..120)

    if (titleError || amountError || installmentCountError) return

    val validCents = amountCents ?: return
    val finalCount = if (isInstallment) (parsedInstallments ?: 2).coerceIn(2, 120) else 1
    val finalRecurrence = if (isRecurring) recurrence else RecurrenceType.NONE
    val chosenDay = DayKey.fromMillis(dateMillis)
    val finalPaymentMethod =
      when (editorType) {
        ExpenseEditorType.DEBIT_ONE_TIME -> ExpensePaymentMethod.DEBIT_ONE_TIME
        ExpenseEditorType.CREDIT_ONE_TIME -> ExpensePaymentMethod.CREDIT_ONE_TIME
        ExpenseEditorType.CREDIT_INSTALLMENT -> ExpensePaymentMethod.CREDIT_INSTALLMENT
        ExpenseEditorType.RECURRING -> ExpensePaymentMethod.RECURRING
      }

    val entity =
      if (existingExpense != null) {
        existingExpense!!.copy(
          title = cleanTitle,
          notes = notes.trim(),
          totalAmountCents = validCents,
          categoryId = selectedCategoryId,
          isInstallment = isInstallment,
          installmentCount = finalCount,
          startYear = if (isInstallment) startYear else chosenDay.year,
          startMonthZeroBased = if (isInstallment) startMonth else chosenDay.monthZeroBased,
          dayOfMonth = if (isInstallment) dayOfMonth else chosenDay.dayOfMonth,
          dateMillis = dateMillis,
          recurrence = finalRecurrence,
          paymentMethod = finalPaymentMethod,
        )
      } else {
        ExpenseEntity(
          title = cleanTitle,
          notes = notes.trim(),
          totalAmountCents = validCents,
          categoryId = selectedCategoryId,
          isInstallment = isInstallment,
          installmentCount = finalCount,
          startYear = if (isInstallment) startYear else chosenDay.year,
          startMonthZeroBased = if (isInstallment) startMonth else chosenDay.monthZeroBased,
          dayOfMonth = if (isInstallment) dayOfMonth else chosenDay.dayOfMonth,
          dateMillis = dateMillis,
          recurrence = finalRecurrence,
          paymentMethod = finalPaymentMethod,
        )
      }
    onSave(entity)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(
              if (existingExpense == null) R.string.expense_editor_new_title
              else R.string.expense_editor_edit_title
            )
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("expense_editor_back_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        actions = {
          TextButton(
            onClick = ::performSave,
            modifier = Modifier.testTag("expense_top_save_button"),
          ) {
            Text(stringResource(R.string.action_save))
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            titleError = false
          },
          label = { Text(stringResource(R.string.expense_title_label)) },
          placeholder = { Text(stringResource(R.string.expense_title_placeholder)) },
          isError = titleError,
          supportingText =
            if (titleError) ({ Text(stringResource(R.string.expense_title_error)) }) else null,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
        )

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            amountError = false
          },
          label = {
            Text(
              stringResource(
                when (editorType) {
                  ExpenseEditorType.DEBIT_ONE_TIME,
                  ExpenseEditorType.CREDIT_ONE_TIME -> R.string.expense_amount_label
                  ExpenseEditorType.CREDIT_INSTALLMENT -> R.string.expense_amount_total_label
                  ExpenseEditorType.RECURRING -> R.string.expense_amount_recurring_label
                }
              )
            )
          },
          placeholder = { Text("0.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = amountError,
          supportingText =
            if (amountError) ({ Text(stringResource(R.string.expense_amount_error)) }) else null,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
        )

        // Expense type: One-time vs Installments vs Recurring
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = stringResource(R.string.expense_payment_type_title),
              style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = editorType == ExpenseEditorType.DEBIT_ONE_TIME,
                onClick = { editorType = ExpenseEditorType.DEBIT_ONE_TIME },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = { Text(stringResource(R.string.expense_type_debit_one_time)) },
                modifier = Modifier.testTag("expense_type_debit_one_time"),
              )
              FilterChip(
                selected = editorType == ExpenseEditorType.CREDIT_ONE_TIME,
                onClick = { editorType = ExpenseEditorType.CREDIT_ONE_TIME },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = { Text(stringResource(R.string.expense_type_credit_one_time)) },
                modifier = Modifier.testTag("expense_type_credit_one_time"),
              )
              FilterChip(
                selected = editorType == ExpenseEditorType.CREDIT_INSTALLMENT,
                onClick = { editorType = ExpenseEditorType.CREDIT_INSTALLMENT },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = { Text(stringResource(R.string.expense_type_credit_installment)) },
                modifier = Modifier.testTag("expense_type_credit_installment"),
              )
              FilterChip(
                selected = editorType == ExpenseEditorType.RECURRING,
                onClick = { editorType = ExpenseEditorType.RECURRING },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = { Text(stringResource(R.string.expense_type_recurring)) },
                modifier = Modifier.testTag("expense_type_recurring"),
              )
            }
          }
        }

        when (editorType) {
          ExpenseEditorType.DEBIT_ONE_TIME,
          ExpenseEditorType.CREDIT_ONE_TIME -> {
            ElevatedCard(
              shape = RoundedCornerShape(20.dp),
              colors =
                CardDefaults.elevatedCardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = stringResource(R.string.expense_date_title),
                  style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                  onClick = { showDatePicker = true },
                  modifier = Modifier.fillMaxWidth().testTag("expense_date_picker_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(DateTimeUtils.formatShortDate(dateMillis))
                }
              }
            }
          }
          ExpenseEditorType.RECURRING -> {
            ElevatedCard(
              shape = RoundedCornerShape(20.dp),
              colors =
                CardDefaults.elevatedCardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.expense_recurring_details_title),
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = stringResource(R.string.expense_recurring_frequency_label),
                  style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  RecurrenceType.entries
                    .filter { it != RecurrenceType.NONE }
                    .forEach { option ->
                      FilterChip(
                        selected = recurrence == option,
                        onClick = { recurrence = option },
                        label = { Text(stringResource(option.labelRes)) },
                        modifier = Modifier.testTag("expense_recurrence_${option.name.lowercase()}"),
                      )
                    }
                }
                Text(
                  text = stringResource(R.string.expense_recurring_start_date_label),
                  style = MaterialTheme.typography.labelLarge,
                )
                OutlinedButton(
                  onClick = { showDatePicker = true },
                  modifier = Modifier.fillMaxWidth().testTag("expense_date_picker_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(DateTimeUtils.formatShortDate(dateMillis))
                }
              }
            }
          }
          ExpenseEditorType.CREDIT_INSTALLMENT -> {
            ElevatedCard(
              shape = RoundedCornerShape(20.dp),
              colors =
                CardDefaults.elevatedCardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.expense_installment_details_title),
                  style = MaterialTheme.typography.titleMedium,
                )

                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  QUICK_INSTALLMENTS.forEach { countOption ->
                    FilterChip(
                      selected = installmentCountText.trim().toIntOrNull() == countOption,
                      onClick = {
                        installmentCountText = countOption.toString()
                        installmentCountError = false
                      },
                      label = { Text("${countOption}x") },
                      modifier = Modifier.testTag("installment_preset_$countOption"),
                    )
                  }
                }

                OutlinedTextField(
                  value = installmentCountText,
                  onValueChange = {
                    installmentCountText = it.filter { ch -> ch.isDigit() }
                    installmentCountError = false
                  },
                  label = { Text(stringResource(R.string.expense_installment_count_label)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  isError = installmentCountError,
                  supportingText =
                    if (installmentCountError) {
                      { Text(stringResource(R.string.expense_installment_count_error)) }
                    } else {
                      null
                    },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("installment_count_input"),
                )

                Text(
                  text = stringResource(R.string.expense_installment_start_month_label),
                  style = MaterialTheme.typography.labelLarge,
                )
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    IconButton(
                      onClick = {
                        val prev = DateTimeUtils.addMonths(startYear, startMonth, -1)
                        startYear = prev.first
                        startMonth = prev.second
                      },
                      modifier = Modifier.testTag("installment_prev_month"),
                    ) {
                      Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.calendar_prev_month),
                      )
                    }
                    Text(
                      text = DateTimeUtils.formatMonthYear(startYear, startMonth),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                      onClick = {
                        val next = DateTimeUtils.addMonths(startYear, startMonth, 1)
                        startYear = next.first
                        startMonth = next.second
                      },
                      modifier = Modifier.testTag("installment_next_month"),
                    ) {
                      Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.calendar_next_month),
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = stringResource(R.string.expense_installment_day_label, dayOfMonth),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                      onClick = { if (dayOfMonth > 1) dayOfMonth-- },
                      enabled = dayOfMonth > 1,
                      modifier = Modifier.testTag("installment_day_minus"),
                    ) {
                      Icon(Icons.Rounded.Remove, contentDescription = null)
                    }
                    FilledTonalIconButton(
                      onClick = { if (dayOfMonth < 31) dayOfMonth++ },
                      enabled = dayOfMonth < 31,
                      modifier = Modifier.testTag("installment_day_plus"),
                    ) {
                      Icon(Icons.Rounded.Add, contentDescription = null)
                    }
                  }
                }

                val previewCents = ExpenseCalculator.parseCurrencyToCents(amountText)
                val previewCount = installmentCountText.trim().toIntOrNull()
                if (
                  previewCents != null &&
                    previewCents > 0L &&
                    previewCount != null &&
                    previewCount in 2..120
                ) {
                  val sliceCents =
                    ExpenseCalculator.installmentAmountCents(previewCents, previewCount, 0)
                  val endMonth = DateTimeUtils.addMonths(startYear, startMonth, previewCount - 1)
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                  ) {
                    Text(
                      text =
                        stringResource(
                          R.string.expense_installment_preview,
                          previewCount,
                          ExpenseCalculator.formatCurrency(sliceCents),
                          DateTimeUtils.formatMonthYearShort(startYear, startMonth),
                          DateTimeUtils.formatMonthYearShort(endMonth.first, endMonth.second),
                        ),
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // Category selector (persistent history across all created categories)
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = stringResource(R.string.expense_category_select_title),
              style = MaterialTheme.typography.titleMedium,
            )
            if (categories.isNotEmpty()) {
              Text(
                text = stringResource(R.string.expense_category_history_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = selectedCategoryId == null,
                onClick = { selectedCategoryId = null },
                label = { Text(stringResource(R.string.expense_category_uncategorized)) },
                modifier = Modifier.testTag("editor_category_none"),
              )
              categories.forEach { cat ->
                val catColor = Color(cat.colorArgb.toInt())
                FilterChip(
                  selected = selectedCategoryId == cat.id,
                  onClick = { selectedCategoryId = cat.id },
                  leadingIcon = {
                    Icon(
                      imageVector = ExpenseCategoryStyleCatalog.iconFor(cat.iconKey),
                      contentDescription = null,
                      tint = catColor,
                      modifier = Modifier.size(18.dp),
                    )
                  },
                  label = { Text(cat.name) },
                  modifier = Modifier.testTag("editor_category_${cat.id}"),
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
                modifier = Modifier.testTag("editor_new_category_button"),
              )
            }

            if (categories.isEmpty()) {
              Text(
                text = stringResource(R.string.expense_category_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(stringResource(R.string.expense_notes_label)) },
          placeholder = { Text(stringResource(R.string.expense_notes_placeholder)) },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("expense_notes_input"),
        )

        Button(
          onClick = ::performSave,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_expense_button"),
        ) {
          Icon(Icons.Rounded.Save, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            stringResource(
              if (existingExpense == null) R.string.expense_save_button
              else R.string.expense_update_button
            )
          )
        }

        Spacer(Modifier.height(24.dp))
      }
    }
  }

  if (showDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.localMillisToUtcPickerMidnight(dateMillis)
      )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { pickedUtc ->
              val cur = Calendar.getInstance().apply { timeInMillis = dateMillis }
              dateMillis =
                DateTimeUtils.mergeDateAndTime(
                  pickedUtc,
                  cur.get(Calendar.HOUR_OF_DAY),
                  cur.get(Calendar.MINUTE),
                )
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showCreateCategoryDialog) {
    CreateExpenseCategoryDialog(
      existingCategories = categories,
      onSelectExisting = { existingId ->
        selectedCategoryId = existingId
        showCreateCategoryDialog = false
      },
      onDismiss = { showCreateCategoryDialog = false },
      onCreate = { name, colorArgb, iconKey ->
        onCreateCategory(name, colorArgb, iconKey) { createdId ->
          selectedCategoryId = createdId
        }
        showCreateCategoryDialog = false
      },
    )
  }
}
