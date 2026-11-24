package com.example.util

import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurrenceType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

data class ExpenseOccurrence(
  val expense: ExpenseEntity,
  val amountCents: Long,
  val dayKey: DayKey,
  val installmentNumber: Int,
  val totalInstallments: Int,
  val isRecurring: Boolean = false,
  val recurrence: RecurrenceType = RecurrenceType.NONE,
  val occurrenceKey: String = if (totalInstallments > 1) "inst_$installmentNumber" else "single",
  val isPaid: Boolean = false,
) {
  val isInstallment: Boolean
    get() = totalInstallments > 1
}

object ExpenseCalculator {
  fun installmentOccurrenceKey(installmentNumber: Int): String = "inst_$installmentNumber"

  fun recurringOccurrenceKey(dayKey: DayKey): String =
    "rec_${dayKey.year}_${dayKey.monthZeroBased}_${dayKey.dayOfMonth}"

  fun singleOccurrenceKey(): String = "single"

  fun formatCurrency(cents: Long): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return fmt.format(cents / 100.0)
  }

  fun centsToEditableString(cents: Long): String {
    if (cents <= 0L) return ""
    val whole = cents / 100L
    val frac = (cents % 100L).toString().padStart(2, '0')
    return "$whole.$frac"
  }

  fun parseCurrencyToCents(raw: String): Long? {
    val cleaned = raw.trim().replace(Regex("[^0-9.,]"), "")
    if (cleaned.isEmpty()) return null

    val lastDot = cleaned.lastIndexOf('.')
    val lastComma = cleaned.lastIndexOf(',')

    val normalized =
      when {
        lastDot >= 0 && lastComma >= 0 -> {
          val decimalPos = maxOf(lastDot, lastComma)
          val integerPart = cleaned.substring(0, decimalPos).replace(".", "").replace(",", "")
          val decimalPart = cleaned.substring(decimalPos + 1).replace(".", "").replace(",", "")
          "$integerPart.$decimalPart"
        }
        lastDot >= 0 || lastComma >= 0 -> {
          val sepPos = maxOf(lastDot, lastComma)
          val after = cleaned.substring(sepPos + 1)
          val before = cleaned.substring(0, sepPos)
          val sepChar = cleaned[sepPos]
          val countSep = cleaned.count { it == sepChar }
          if (countSep == 1 && after.length <= 2) {
            "$before.$after"
          } else if (countSep > 1) {
            cleaned.replace(sepChar.toString(), "")
          } else {
            // Single separator with 3+ trailing digits (likely thousands separator)
            "$before$after"
          }
        }
        else -> cleaned
      }

    val value = normalized.toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    return (value * 100.0).roundToLong()
  }

  fun installmentAmountCents(totalCents: Long, installmentCount: Int, zeroBasedIndex: Int): Long {
    val count = installmentCount.coerceAtLeast(1)
    val base = totalCents / count
    val remainder = (totalCents % count).toInt()
    return base + if (zeroBasedIndex < remainder) 1L else 0L
  }

  fun installmentDayForOffset(
    startYear: Int,
    startMonthZeroBased: Int,
    desiredDayOfMonth: Int,
    monthOffset: Int,
  ): DayKey {
    val cal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, startYear)
        set(Calendar.MONTH, startMonthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
      }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val clampedDay = desiredDayOfMonth.coerceIn(1, maxDay)
    return DayKey(
      year = cal.get(Calendar.YEAR),
      monthZeroBased = cal.get(Calendar.MONTH),
      dayOfMonth = clampedDay,
    )
  }

  fun installmentScheduleForPlan(
    expense: ExpenseEntity,
    paidKeys: Set<Pair<Long, String>> = emptySet(),
  ): List<ExpenseOccurrence> {
    if (!expense.isInstallment || expense.installmentCount <= 1) return emptyList()
    return (0 until expense.installmentCount).map { offset ->
      val num = offset + 1
      val key = installmentOccurrenceKey(num)
      val dayKey =
        installmentDayForOffset(
          startYear = expense.startYear,
          startMonthZeroBased = expense.startMonthZeroBased,
          desiredDayOfMonth = expense.dayOfMonth,
          monthOffset = offset,
        )
      ExpenseOccurrence(
        expense = expense,
        amountCents =
          installmentAmountCents(
            totalCents = expense.totalAmountCents,
            installmentCount = expense.installmentCount,
            zeroBasedIndex = offset,
          ),
        dayKey = dayKey,
        installmentNumber = num,
        totalInstallments = expense.installmentCount,
        isRecurring = false,
        recurrence = RecurrenceType.NONE,
        occurrenceKey = key,
        isPaid = paidKeys.contains(expense.id to key),
      )
    }
  }

  fun occurrencesInMonth(
    expenses: List<ExpenseEntity>,
    year: Int,
    monthZeroBased: Int,
    paidKeys: Set<Pair<Long, String>> = emptySet(),
  ): List<ExpenseOccurrence> {
    val targetTotalMonths = year * 12 + monthZeroBased
    val monthStartMillis = DayKey(year, monthZeroBased, 1).startOfDayMillis()
    val result = mutableListOf<ExpenseOccurrence>()

    for (expense in expenses) {
      if (expense.isInstallment && expense.installmentCount > 1) {
        val startTotalMonths = expense.startYear * 12 + expense.startMonthZeroBased
        val diff = targetTotalMonths - startTotalMonths
        if (diff in 0 until expense.installmentCount) {
          val dayKey =
            installmentDayForOffset(
              startYear = expense.startYear,
              startMonthZeroBased = expense.startMonthZeroBased,
              desiredDayOfMonth = expense.dayOfMonth,
              monthOffset = diff,
            )
          val amount =
            installmentAmountCents(
              totalCents = expense.totalAmountCents,
              installmentCount = expense.installmentCount,
              zeroBasedIndex = diff,
            )
          val num = diff + 1
          val key = installmentOccurrenceKey(num)
          result.add(
            ExpenseOccurrence(
              expense = expense,
              amountCents = amount,
              dayKey = dayKey,
              installmentNumber = num,
              totalInstallments = expense.installmentCount,
              isRecurring = false,
              recurrence = RecurrenceType.NONE,
              occurrenceKey = key,
              isPaid = paidKeys.contains(expense.id to key),
            )
          )
        }
      } else if (expense.recurrence != RecurrenceType.NONE) {
        val baseDay = DayKey.fromMillis(expense.dateMillis)
        val baseTotalMonths = baseDay.year * 12 + baseDay.monthZeroBased
        if (baseTotalMonths > targetTotalMonths) continue

        when (expense.recurrence) {
          RecurrenceType.MONTHLY -> {
            val diff = targetTotalMonths - baseTotalMonths
            if (diff >= 0) {
              val occDay =
                installmentDayForOffset(
                  startYear = baseDay.year,
                  startMonthZeroBased = baseDay.monthZeroBased,
                  desiredDayOfMonth = baseDay.dayOfMonth,
                  monthOffset = diff,
                )
              val key = recurringOccurrenceKey(occDay)
              result.add(
                ExpenseOccurrence(
                  expense = expense,
                  amountCents = expense.totalAmountCents,
                  dayKey = occDay,
                  installmentNumber = 1,
                  totalInstallments = 1,
                  isRecurring = true,
                  recurrence = expense.recurrence,
                  occurrenceKey = key,
                  isPaid = paidKeys.contains(expense.id to key),
                )
              )
            }
          }
          RecurrenceType.YEARLY -> {
            val diffYears = year - baseDay.year
            if (diffYears >= 0 && baseDay.monthZeroBased == monthZeroBased) {
              val occDay =
                installmentDayForOffset(
                  startYear = year,
                  startMonthZeroBased = monthZeroBased,
                  desiredDayOfMonth = baseDay.dayOfMonth,
                  monthOffset = 0,
                )
              val key = recurringOccurrenceKey(occDay)
              result.add(
                ExpenseOccurrence(
                  expense = expense,
                  amountCents = expense.totalAmountCents,
                  dayKey = occDay,
                  installmentNumber = 1,
                  totalInstallments = 1,
                  isRecurring = true,
                  recurrence = expense.recurrence,
                  occurrenceKey = key,
                  isPaid = paidKeys.contains(expense.id to key),
                )
              )
            }
          }
          RecurrenceType.DAILY -> {
            val baseCal = baseDay.toCalendarAt(12, 0)
            val approxDays =
              ((monthStartMillis - baseDay.startOfDayMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
            val startStep = (approxDays - 2).coerceAtLeast(0)
            for (step in startStep..(startStep + 40)) {
              val cal = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, step) }
              val occYear = cal.get(Calendar.YEAR)
              val occMonth = cal.get(Calendar.MONTH)
              val occTotalMonths = occYear * 12 + occMonth
              if (occTotalMonths > targetTotalMonths) break
              if (occTotalMonths == targetTotalMonths) {
                val occDay = DayKey(occYear, occMonth, cal.get(Calendar.DAY_OF_MONTH))
                val key = recurringOccurrenceKey(occDay)
                result.add(
                  ExpenseOccurrence(
                    expense = expense,
                    amountCents = expense.totalAmountCents,
                    dayKey = occDay,
                    installmentNumber = 1,
                    totalInstallments = 1,
                    isRecurring = true,
                    recurrence = expense.recurrence,
                    occurrenceKey = key,
                    isPaid = paidKeys.contains(expense.id to key),
                  )
                )
              }
            }
          }
          RecurrenceType.WEEKLY -> {
            val baseCal = baseDay.toCalendarAt(12, 0)
            val approxWeeks =
              ((monthStartMillis - baseDay.startOfDayMillis()) / (7L * 86_400_000L))
                .toInt()
                .coerceAtLeast(0)
            val startStep = (approxWeeks - 2).coerceAtLeast(0)
            for (step in startStep..(startStep + 10)) {
              val cal = (baseCal.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, step) }
              val occYear = cal.get(Calendar.YEAR)
              val occMonth = cal.get(Calendar.MONTH)
              val occTotalMonths = occYear * 12 + occMonth
              if (occTotalMonths > targetTotalMonths) break
              if (occTotalMonths == targetTotalMonths) {
                val occDay = DayKey(occYear, occMonth, cal.get(Calendar.DAY_OF_MONTH))
                val key = recurringOccurrenceKey(occDay)
                result.add(
                  ExpenseOccurrence(
                    expense = expense,
                    amountCents = expense.totalAmountCents,
                    dayKey = occDay,
                    installmentNumber = 1,
                    totalInstallments = 1,
                    isRecurring = true,
                    recurrence = expense.recurrence,
                    occurrenceKey = key,
                    isPaid = paidKeys.contains(expense.id to key),
                  )
                )
              }
            }
          }
          RecurrenceType.NONE -> {}
        }
      } else {
        val dayKey = DayKey.fromMillis(expense.dateMillis)
        if (dayKey.year == year && dayKey.monthZeroBased == monthZeroBased) {
          val key = singleOccurrenceKey()
          result.add(
            ExpenseOccurrence(
              expense = expense,
              amountCents = expense.totalAmountCents,
              dayKey = dayKey,
              installmentNumber = 1,
              totalInstallments = 1,
              isRecurring = false,
              recurrence = RecurrenceType.NONE,
              occurrenceKey = key,
              isPaid = paidKeys.contains(expense.id to key),
            )
          )
        }
      }
    }

    return result.sortedWith(
      compareBy<ExpenseOccurrence> { it.dayKey.dayOfMonth }
        .thenByDescending { it.expense.createdAtMillis }
    )
  }

  fun occurrencesInCalendarWindow(
    expenses: List<ExpenseEntity>,
    displayedYear: Int,
    displayedMonthZeroBased: Int,
    paidKeys: Set<Pair<Long, String>> = emptySet(),
  ): List<ExpenseOccurrence> {
    val prev = DateTimeUtils.addMonths(displayedYear, displayedMonthZeroBased, -1)
    val next = DateTimeUtils.addMonths(displayedYear, displayedMonthZeroBased, 1)
    return buildList {
      addAll(occurrencesInMonth(expenses, prev.first, prev.second, paidKeys))
      addAll(occurrencesInMonth(expenses, displayedYear, displayedMonthZeroBased, paidKeys))
      addAll(occurrencesInMonth(expenses, next.first, next.second, paidKeys))
    }
  }
}
