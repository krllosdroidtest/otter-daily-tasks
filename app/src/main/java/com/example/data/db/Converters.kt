package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.AlarmSoundMode
import com.example.data.model.ExpensePaymentMethod
import com.example.data.model.RecurrenceType

class Converters {
  @TypeConverter
  fun fromRecurrence(value: RecurrenceType): String = value.name

  @TypeConverter
  fun toRecurrence(value: String): RecurrenceType =
    runCatching { RecurrenceType.valueOf(value) }.getOrDefault(RecurrenceType.NONE)

  @TypeConverter
  fun fromAlarmSoundMode(value: AlarmSoundMode): String = value.name

  @TypeConverter
  fun toAlarmSoundMode(value: String): AlarmSoundMode =
    runCatching { AlarmSoundMode.valueOf(value) }.getOrDefault(AlarmSoundMode.SYSTEM)

  @TypeConverter
  fun fromExpensePaymentMethod(value: ExpensePaymentMethod): String = value.name

  @TypeConverter
  fun toExpensePaymentMethod(value: String): ExpensePaymentMethod =
    runCatching { ExpensePaymentMethod.valueOf(value) }
      .getOrDefault(ExpensePaymentMethod.DEBIT_ONE_TIME)
}
