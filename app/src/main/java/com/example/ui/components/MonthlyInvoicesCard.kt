package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpensePaymentMethod
import com.example.util.DateTimeUtils
import com.example.util.ExpenseCalculator
import com.example.util.InvoiceCardSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyInvoicesCard(
  displayedYear: Int,
  displayedMonth: Int,
  invoices: List<InvoiceCardSummary>,
  totalInvoicesCents: Long,
  paidInvoicesCents: Long,
  onToggleInvoicePaid: (InvoiceCardSummary, Boolean) -> Unit,
  onToggleOccurrencePaid: (expenseId: Long, occurrenceKey: String, markAsPaid: Boolean) -> Unit,
  onEditExpense: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val monthLabel = DateTimeUtils.formatMonthYear(displayedYear, displayedMonth)

  ElevatedCard(
    modifier = modifier.fillMaxWidth().testTag("monthly_invoices_card"),
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.expense_invoices_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.expense_invoices_subtitle, monthLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = ExpenseCalculator.formatCurrency(totalInvoicesCents),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.testTag("expense_month_invoices_total_text"),
          )
          if (paidInvoicesCents > 0L) {
            Text(
              text =
                stringResource(
                  R.string.expense_invoices_paid_total,
                  ExpenseCalculator.formatCurrency(paidInvoicesCents),
                ),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      if (invoices.isEmpty()) {
        Text(
          text = stringResource(R.string.expense_invoices_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          invoices.forEach { invoice ->
            CardInvoiceRow(
              invoice = invoice,
              onToggleInvoicePaid = { onToggleInvoicePaid(invoice, !invoice.allPaid) },
              onToggleOccurrencePaid = onToggleOccurrencePaid,
              onEditExpense = onEditExpense,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardInvoiceRow(
  invoice: InvoiceCardSummary,
  onToggleInvoicePaid: () -> Unit,
  onToggleOccurrencePaid: (expenseId: Long, occurrenceKey: String, markAsPaid: Boolean) -> Unit,
  onEditExpense: (Long) -> Unit,
) {
  val cardKey = invoice.card?.id ?: -1L
  var expanded by remember(cardKey, invoice.billingYear, invoice.billingMonthZeroBased) {
    mutableStateOf(false)
  }
  val cardColor =
    invoice.card?.let { Color(it.colorArgb.toInt()) } ?: MaterialTheme.colorScheme.outline
  val cardName = invoice.card?.name ?: stringResource(R.string.credit_card_none)

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth().testTag("invoice_card_${cardKey}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier.size(36.dp).clip(CircleShape).background(cardColor.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.CreditCard,
            contentDescription = null,
            tint = cardColor,
            modifier = Modifier.size(18.dp),
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = cardName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              if (invoice.card != null) {
                "${stringResource(R.string.expense_invoice_due_label, DateTimeUtils.formatShortDate(invoice.dueDayKey.startOfDayMillis()))} • ${stringResource(R.string.expense_installment_paid_progress, invoice.paidCount, invoice.occurrences.size)}"
              } else {
                stringResource(
                  R.string.expense_installment_paid_progress,
                  invoice.paidCount,
                  invoice.occurrences.size,
                )
              },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Text(
          text = ExpenseCalculator.formatCurrency(invoice.totalCents),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color =
            if (invoice.allPaid) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        TextButton(
          onClick = { expanded = !expanded },
          modifier = Modifier.testTag("expand_invoice_${cardKey}"),
        ) {
          Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            stringResource(
              if (expanded) R.string.expense_invoice_hide_items
              else R.string.expense_invoice_show_items,
              invoice.occurrences.size,
            )
          )
        }

        FilterChip(
          selected = invoice.allPaid,
          onClick = onToggleInvoicePaid,
          leadingIcon = {
            Icon(
              imageVector =
                if (invoice.allPaid) Icons.Rounded.CheckCircle
                else Icons.Rounded.RadioButtonUnchecked,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          label = {
            Text(
              stringResource(
                if (invoice.allPaid) R.string.expense_invoice_paid_all
                else R.string.expense_invoice_pay_all
              )
            )
          },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            ),
          modifier = Modifier.testTag("toggle_invoice_paid_${cardKey}"),
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          invoice.occurrences.forEach { occ ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color =
                if (occ.isPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onEditExpense(occ.expense.id) }
                  .testTag("invoice_item_${occ.expense.id}_${occ.occurrenceKey}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                  modifier =
                    Modifier.size(22.dp).clickable {
                      onToggleOccurrencePaid(occ.expense.id, occ.occurrenceKey, !occ.isPaid)
                    },
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = occ.expense.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val subtitle =
                    if (occ.paymentMethod == ExpensePaymentMethod.CREDIT_INSTALLMENT) {
                      "${stringResource(R.string.expense_installment_badge, occ.installmentNumber, occ.totalInstallments)} • ${stringResource(R.string.expense_invoice_purchase_date, DateTimeUtils.formatShortDate(occ.expense.dateMillis))}"
                    } else {
                      "${stringResource(R.string.expense_type_credit_one_time)} • ${stringResource(R.string.expense_invoice_purchase_date, DateTimeUtils.formatShortDate(occ.expense.dateMillis))}"
                    }
                  Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = ExpenseCalculator.formatCurrency(occ.amountCents),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (occ.isPaid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }
    }
  }
}
