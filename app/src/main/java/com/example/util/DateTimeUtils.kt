package com.example.util

import android.content.Context
import android.text.format.DateFormat
import com.example.R
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskEntity
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DayKey(val year: Int, val monthZeroBased: Int, val dayOfMonth: Int) {
  fun toCalendarAt(hourOfDay: Int = 9, minute: Int = 0): Calendar =
    Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, dayOfMonth)
      set(Calendar.HOUR_OF_DAY, hourOfDay)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

  fun startOfDayMillis(): Long =
    Calendar.getInstance()
      .apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      .timeInMillis

  fun endOfDayMillis(): Long =
    Calendar.getInstance()
      .apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }
      .timeInMillis

  companion object {
    fun today(): DayKey = fromMillis(System.currentTimeMillis())

    fun fromMillis(millis: Long): DayKey {
      val cal = Calendar.getInstance().apply { timeInMillis = millis }
      return DayKey(
        year = cal.get(Calendar.YEAR),
        monthZeroBased = cal.get(Calendar.MONTH),
        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
      )
    }
  }
}

object DateTimeUtils {
  fun formatShortDate(millis: Long): String {
    val locale = Locale.getDefault()
    val fmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
    return fmt.format(Date(millis))
  }

  fun formatTime(millis: Long): String {
    val locale = Locale.getDefault()
    val fmt = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale)
    return fmt.format(Date(millis))
  }

  fun formatDateTime(millis: Long): String {
    val locale = Locale.getDefault()
    val fmt =
      java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT,
        locale,
      )
    return fmt.format(Date(millis))
  }

  fun formatMonthYear(year: Int, monthZeroBased: Int): String {
    val locale = Locale.getDefault()
    val cal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
      }
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMM yyyy")
    val fmt = SimpleDateFormat(pattern, locale)
    val raw = fmt.format(cal.time)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
  }

  fun formatMonthYearShort(year: Int, monthZeroBased: Int): String {
    val locale = Locale.getDefault()
    val cal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
      }
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMM yyyy")
    val fmt = SimpleDateFormat(pattern, locale)
    val raw = fmt.format(cal.time)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
  }

  fun addMonths(year: Int, monthZeroBased: Int, delta: Int): Pair<Int, Int> {
    val cal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, delta)
      }
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
  }

  fun formatDayHeader(dayKey: DayKey): String {
    val locale = Locale.getDefault()
    val cal = dayKey.toCalendarAt()
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEE, d MMMM")
    val fmt = SimpleDateFormat(pattern, locale)
    val raw = fmt.format(cal.time)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
  }

  fun weekdayInitials(): List<String> {
    val locale = Locale.getDefault()
    val symbols = DateFormatSymbols.getInstance(locale).shortWeekdays
    // Calendar.SUNDAY = 1 .. Calendar.SATURDAY = 7
    return (Calendar.SUNDAY..Calendar.SATURDAY).map { dayConst ->
      val raw = symbols.getOrNull(dayConst)?.trim().orEmpty()
      raw.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
  }

  fun buildMonthGrid(year: Int, monthZeroBased: Int): List<DayKey?> {
    val cal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
      }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1 .. Saturday = 7
    val leadingBlanks = firstDayOfWeek - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = ArrayList<DayKey?>(42)
    repeat(leadingBlanks) { cells.add(null) }
    for (day in 1..daysInMonth) {
      cells.add(DayKey(year, monthZeroBased, day))
    }
    while (cells.size % 7 != 0) {
      cells.add(null)
    }
    return cells
  }

  fun buildWeekAround(dayKey: DayKey): List<DayKey> {
    val cal = dayKey.toCalendarAt()
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.SUNDAY))
    return (0 until 7).map {
      val item =
        DayKey(
          year = cal.get(Calendar.YEAR),
          monthZeroBased = cal.get(Calendar.MONTH),
          dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
        )
      cal.add(Calendar.DAY_OF_MONTH, 1)
      item
    }
  }

  fun nextOccurrenceMillis(currentDueMillis: Long, recurrence: RecurrenceType): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = currentDueMillis }
    when (recurrence) {
      RecurrenceType.NONE -> {}
      RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
      RecurrenceType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
      RecurrenceType.MONTHLY -> cal.add(Calendar.MONTH, 1)
      RecurrenceType.YEARLY -> cal.add(Calendar.YEAR, 1)
    }
    return cal.timeInMillis
  }

  /**
   * Generates all occurrence timestamps (base + future recurrences) of [task] within
   * [rangeStartMillis]..[rangeEndMillis]. Completed or non-repeating tasks only match their own
   * [TaskEntity.dueAtMillis].
   */
  fun generateOccurrencesInRange(
    task: TaskEntity,
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    maxProjectedOccurrences: Int = 400,
  ): List<Long> {
    if (task.dueAtMillis > rangeEndMillis) return emptyList()

    if (task.isCompleted || task.recurrence == RecurrenceType.NONE) {
      return if (task.dueAtMillis in rangeStartMillis..rangeEndMillis) {
        listOf(task.dueAtMillis)
      } else {
        emptyList()
      }
    }

    val results = mutableListOf<Long>()
    val baseCal = Calendar.getInstance().apply { timeInMillis = task.dueAtMillis }
    val field =
      when (task.recurrence) {
        RecurrenceType.DAILY -> Calendar.DAY_OF_YEAR
        RecurrenceType.WEEKLY -> Calendar.WEEK_OF_YEAR
        RecurrenceType.MONTHLY -> Calendar.MONTH
        RecurrenceType.YEARLY -> Calendar.YEAR
        RecurrenceType.NONE -> return emptyList()
      }

    for (step in 0..maxProjectedOccurrences) {
      val cal = (baseCal.clone() as Calendar).apply { if (step > 0) add(field, step) }
      val millis = cal.timeInMillis
      if (millis > rangeEndMillis) break
      if (millis >= rangeStartMillis) {
        results.add(millis)
      }
    }
    return results
  }

  fun monthRangeWithWeekBuffer(year: Int, monthZeroBased: Int): Pair<Long, Long> {
    val startCal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -7)
      }
    val endCal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        add(Calendar.DAY_OF_YEAR, 7)
      }
    return startCal.timeInMillis to endCal.timeInMillis
  }

  fun formatReminderOffset(context: Context, minutes: Int): String =
    when {
      minutes <= 0 -> context.getString(R.string.reminder_on_time)
      minutes < 60 -> context.getString(R.string.reminder_minutes_before, minutes)
      minutes == 60 -> context.getString(R.string.reminder_one_hour_before)
      minutes < 1440 -> {
        val hours = minutes / 60
        val rem = minutes % 60
        if (rem == 0) context.getString(R.string.reminder_hours_before, hours)
        else context.getString(R.string.reminder_hours_minutes_before, hours, rem)
      }
      minutes == 1440 -> context.getString(R.string.reminder_one_day_before)
      minutes < 10080 -> {
        val days = minutes / 1440
        val remHours = (minutes % 1440) / 60
        if (remHours == 0) context.getString(R.string.reminder_days_before, days)
        else context.getString(R.string.reminder_days_hours_before, days, remHours)
      }
      else -> context.getString(R.string.reminder_one_week_before)
    }

  fun mergeDateAndTime(dateMillisUtcFromPicker: Long, hour: Int, minute: Int): Long {
    val utcCal =
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateMillisUtcFromPicker
      }
    val localCal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    return localCal.timeInMillis
  }

  fun localMillisToUtcPickerMidnight(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal =
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, localCal.get(Calendar.YEAR))
        set(Calendar.MONTH, localCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
      }
    return utcCal.timeInMillis
  }
}
