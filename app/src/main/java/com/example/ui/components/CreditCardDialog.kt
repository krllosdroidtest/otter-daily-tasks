package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseCategoryStyleCatalog
import com.example.util.DateTimeUtils
import com.example.util.DayKey
import com.example.util.ExpenseCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreditCardDialog(
  editingCard: CreditCardEntity? = null,
  onDismiss: () -> Unit,
  onSave: (CreditCardEntity) -> Unit,
) {
  var name by remember(editingCard) { mutableStateOf(editingCard?.name ?: "") }
  var selectedColorArgb by remember(editingCard) {
    mutableLongStateOf(editingCard?.colorArgb ?: ExpenseCategoryStyleCatalog.colors[2].argb)
  }
  var closingDay by remember(editingCard) { mutableIntStateOf(editingCard?.closingDay ?: 25) }
  var dueDay by remember(editingCard) { mutableIntStateOf(editingCard?.dueDay ?: 5) }
  var nameError by remember { mutableStateOf(false) }

  val today = remember { DayKey.today() }
  val previewBilling =
    remember(today, closingDay, dueDay) {
      ExpenseCalculator.suggestCreditBillingMonth(today, closingDay, dueDay)
    }
  val previewDueDayKey =
    remember(previewBilling) {
      ExpenseCalculator.installmentDayForOffset(
        previewBilling.billingYear,
        previewBilling.billingMonthZeroBased,
        previewBilling.dueDayOfMonth,
        0,
      )
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.CreditCard,
          contentDescription = null,
          tint = Color(selectedColorArgb.toInt()),
        )
        Text(
          stringResource(
            if (editingCard == null) R.string.credit_card_create_title
            else R.string.credit_card_edit_title
          )
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            nameError = false
          },
          label = { Text(stringResource(R.string.credit_card_name_label)) },
          placeholder = { Text(stringResource(R.string.credit_card_name_placeholder)) },
          singleLine = true,
          isError = nameError,
          supportingText =
            if (nameError) ({ Text(stringResource(R.string.credit_card_name_error)) }) else null,
          modifier = Modifier.fillMaxWidth().testTag("credit_card_name_input"),
        )

        Text(
          text = stringResource(R.string.credit_card_color_label),
          style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ExpenseCategoryStyleCatalog.colors.forEachIndexed { idx, colorOption ->
            val isSelected = selectedColorArgb == colorOption.argb
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .clip(CircleShape)
                  .clickable { selectedColorArgb = colorOption.argb }
                  .padding(4.dp)
                  .testTag("credit_card_color_$idx"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.size(34.dp)
                    .clip(CircleShape)
                    .background(colorOption.color)
                    .then(
                      if (isSelected) {
                        Modifier.border(
                          BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface),
                          CircleShape,
                        )
                      } else {
                        Modifier
                      }
                    ),
                contentAlignment = Alignment.Center,
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.credit_card_closing_day_label, closingDay),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(
              onClick = { if (closingDay > 1) closingDay-- },
              enabled = closingDay > 1,
              modifier = Modifier.testTag("card_closing_minus"),
            ) {
              Icon(Icons.Rounded.Remove, contentDescription = null)
            }
            FilledTonalIconButton(
              onClick = { if (closingDay < 31) closingDay++ },
              enabled = closingDay < 31,
              modifier = Modifier.testTag("card_closing_plus"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.credit_card_due_day_label, dueDay),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(
              onClick = { if (dueDay > 1) dueDay-- },
              enabled = dueDay > 1,
              modifier = Modifier.testTag("card_due_minus"),
            ) {
              Icon(Icons.Rounded.Remove, contentDescription = null)
            }
            FilledTonalIconButton(
              onClick = { if (dueDay < 31) dueDay++ },
              enabled = dueDay < 31,
              modifier = Modifier.testTag("card_due_plus"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        ) {
          Text(
            text =
              stringResource(
                R.string.credit_card_cycle_preview,
                DateTimeUtils.formatShortDate(today.startOfDayMillis()),
                DateTimeUtils.formatMonthYearShort(
                  previewBilling.billingYear,
                  previewBilling.billingMonthZeroBased,
                ),
                DateTimeUtils.formatShortDate(previewDueDayKey.startOfDayMillis()),
              ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = name.trim()
          if (clean.isEmpty()) {
            nameError = true
          } else {
            val saved =
              editingCard?.copy(
                name = clean,
                colorArgb = selectedColorArgb,
                closingDay = closingDay,
                dueDay = dueDay,
              )
                ?: CreditCardEntity(
                  name = clean,
                  colorArgb = selectedColorArgb,
                  closingDay = closingDay,
                  dueDay = dueDay,
                )
            onSave(saved)
          }
        },
        modifier = Modifier.testTag("credit_card_save_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
