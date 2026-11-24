package com.example.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.model.AlarmSoundMode
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentEntity
import com.example.data.model.ExpensePaymentMethod
import com.example.data.model.RecurrenceType
import com.example.data.model.ShoppingFolderEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithAttachments
import com.example.data.repository.ThemePreferences
import com.example.ui.theme.AppPalette
import com.example.ui.theme.ThemeMode
import com.example.util.AttachmentStorage
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
  val exportedAtMillis: Long,
  val preferences: ThemePreferences,
  val tasks: List<Pair<TaskEntity, List<TaskAttachmentEntity>>>,
  val shoppingItems: List<ShoppingItemEntity>,
  val shoppingFolders: List<ShoppingFolderEntity> = emptyList(),
  val expenseCategories: List<ExpenseCategoryEntity> = emptyList(),
  val expenses: List<ExpenseEntity> = emptyList(),
  val expensePayments: List<ExpensePaymentEntity> = emptyList(),
)

data class BackupPreview(
  val exportedAtMillis: Long,
  val taskCount: Int,
  val attachmentCount: Int,
  val shoppingItemCount: Int,
  val payload: BackupPayload,
  val folderCount: Int = 0,
  val expenseCount: Int = 0,
)

object BackupManager {
  private const val MAX_ATTACHMENT_EMBED_BYTES = 5L * 1024L * 1024L

  fun exportToUri(
    context: Context,
    targetUri: Uri,
    preferences: ThemePreferences,
    tasks: List<TaskWithAttachments>,
    shoppingItems: List<ShoppingItemEntity>,
    shoppingFolders: List<ShoppingFolderEntity> = emptyList(),
    expenseCategories: List<ExpenseCategoryEntity> = emptyList(),
    expenses: List<ExpenseEntity> = emptyList(),
    expensePayments: List<ExpensePaymentEntity> = emptyList(),
  ): Result<Int> = runCatching {
    val root = JSONObject()
    root.put("schemaVersion", 4)
    root.put("app", "otter")
    root.put("exportedAtMillis", System.currentTimeMillis())

    val prefsJson =
      JSONObject().apply {
        put("themeMode", preferences.themeMode.name)
        put("useDynamicColor", preferences.useDynamicColor)
        put("palette", preferences.palette.name)
      }
    root.put("preferences", prefsJson)

    val tasksArray = JSONArray()
    for (entry in tasks) {
      val t = entry.task
      val tJson =
        JSONObject().apply {
          put("title", t.title)
          put("description", t.description)
          put("dueAtMillis", t.dueAtMillis)
          put("recurrence", t.recurrence.name)
          put("reminderOffsetMinutes", t.reminderOffsetMinutes)
          put("alarmEnabled", t.alarmEnabled)
          put("alarmSoundMode", t.alarmSoundMode.name)
          put("alarmSoundUri", t.alarmSoundUri ?: JSONObject.NULL)
          put("alarmSoundTitle", t.alarmSoundTitle)
          put("vibrate", t.vibrate)
          put("isCompleted", t.isCompleted)
          put("completedAtMillis", t.completedAtMillis ?: JSONObject.NULL)
          put("createdAtMillis", t.createdAtMillis)
        }

      val attArray = JSONArray()
      for (att in entry.attachments) {
        val aJson =
          JSONObject().apply {
            put("displayName", att.displayName)
            put("mimeType", att.mimeType)
            put("sizeBytes", att.sizeBytes)
            val file = File(att.localFilePath)
            if (file.exists() && file.length() <= MAX_ATTACHMENT_EMBED_BYTES) {
              val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
              put("base64", encoded)
            }
          }
        attArray.put(aJson)
      }
      tJson.put("attachments", attArray)
      tasksArray.put(tJson)
    }
    root.put("tasks", tasksArray)

    val foldersArray = JSONArray()
    for (folder in shoppingFolders) {
      foldersArray.put(
        JSONObject().apply {
          put("id", folder.id)
          put("name", folder.name)
          put("createdAtMillis", folder.createdAtMillis)
        }
      )
    }
    root.put("shoppingFolders", foldersArray)

    val shopArray = JSONArray()
    for (item in shoppingItems) {
      shopArray.put(
        JSONObject().apply {
          put("folderId", item.folderId ?: JSONObject.NULL)
          put("name", item.name)
          put("quantity", item.quantity)
          put("isChecked", item.isChecked)
          put("createdAtMillis", item.createdAtMillis)
        }
      )
    }
    root.put("shoppingItems", shopArray)

    val categoriesArray = JSONArray()
    for (cat in expenseCategories) {
      categoriesArray.put(
        JSONObject().apply {
          put("id", cat.id)
          put("name", cat.name)
          put("colorArgb", cat.colorArgb)
          put("iconKey", cat.iconKey)
          put("createdAtMillis", cat.createdAtMillis)
        }
      )
    }
    root.put("expenseCategories", categoriesArray)

    val expensesArray = JSONArray()
    for (exp in expenses) {
      expensesArray.put(
        JSONObject().apply {
          put("id", exp.id)
          put("title", exp.title)
          put("notes", exp.notes)
          put("totalAmountCents", exp.totalAmountCents)
          put("categoryId", exp.categoryId ?: JSONObject.NULL)
          put("isInstallment", exp.isInstallment)
          put("installmentCount", exp.installmentCount)
          put("startYear", exp.startYear)
          put("startMonthZeroBased", exp.startMonthZeroBased)
          put("dayOfMonth", exp.dayOfMonth)
          put("dateMillis", exp.dateMillis)
          put("recurrence", exp.recurrence.name)
          put("paymentMethod", exp.effectivePaymentMethod().name)
          put("createdAtMillis", exp.createdAtMillis)
        }
      )
    }
    root.put("expenses", expensesArray)

    val paymentsArray = JSONArray()
    for (payment in expensePayments) {
      paymentsArray.put(
        JSONObject().apply {
          put("expenseId", payment.expenseId)
          put("occurrenceKey", payment.occurrenceKey)
          put("paidAtMillis", payment.paidAtMillis)
        }
      )
    }
    root.put("expensePayments", paymentsArray)

    val jsonString = root.toString(2)
    context.contentResolver.openOutputStream(targetUri)?.use { out ->
      out.write(jsonString.toByteArray(Charsets.UTF_8))
      out.flush()
    } ?: error(context.getString(com.example.R.string.err_write_backup))

    tasks.size + shoppingItems.size + expenses.size
  }

  fun parseFromUri(context: Context, sourceUri: Uri): Result<BackupPreview> = runCatching {
    val raw =
      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        input.bufferedReader(Charsets.UTF_8).readText()
      } ?: error(context.getString(com.example.R.string.err_open_backup))

    val root = JSONObject(raw)
    val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

    val prefsObj = root.optJSONObject("preferences")
    val prefs =
      if (prefsObj != null) {
        ThemePreferences(
          themeMode =
            runCatching { ThemeMode.valueOf(prefsObj.optString("themeMode")) }
              .getOrDefault(ThemeMode.SYSTEM),
          useDynamicColor = prefsObj.optBoolean("useDynamicColor", true),
          palette =
            runCatching { AppPalette.valueOf(prefsObj.optString("palette")) }
              .getOrDefault(AppPalette.OCEAN_TEAL),
        )
      } else {
        ThemePreferences()
      }

    var totalAttachments = 0
    val tasks = mutableListOf<Pair<TaskEntity, List<TaskAttachmentEntity>>>()
    val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
    for (i in 0 until tasksArray.length()) {
      val tJson = tasksArray.getJSONObject(i)
      val rawSoundUri =
        if (tJson.isNull("alarmSoundUri")) null else tJson.optString("alarmSoundUri", "")
      val soundUri = rawSoundUri?.takeIf { it.isNotBlank() }
      val completedAt =
        if (tJson.isNull("completedAtMillis")) null else tJson.optLong("completedAtMillis")

      val task =
        TaskEntity(
          title = tJson.optString("title", context.getString(com.example.R.string.default_task_name)),
          description = tJson.optString("description", ""),
          dueAtMillis = tJson.optLong("dueAtMillis", System.currentTimeMillis()),
          recurrence =
            runCatching { RecurrenceType.valueOf(tJson.optString("recurrence")) }
              .getOrDefault(RecurrenceType.NONE),
          reminderOffsetMinutes = tJson.optInt("reminderOffsetMinutes", 0).coerceIn(0, 10080),
          alarmEnabled = tJson.optBoolean("alarmEnabled", true),
          alarmSoundMode =
            runCatching { AlarmSoundMode.valueOf(tJson.optString("alarmSoundMode")) }
              .getOrDefault(AlarmSoundMode.SYSTEM),
          alarmSoundUri = soundUri,
          alarmSoundTitle = tJson.optString("alarmSoundTitle", ""),
          vibrate = tJson.optBoolean("vibrate", true),
          isCompleted = tJson.optBoolean("isCompleted", false),
          completedAtMillis = completedAt,
          createdAtMillis = tJson.optLong("createdAtMillis", System.currentTimeMillis()),
        )

      val attachments = mutableListOf<TaskAttachmentEntity>()
      val attArray = tJson.optJSONArray("attachments") ?: JSONArray()
      for (j in 0 until attArray.length()) {
        val aJson = attArray.getJSONObject(j)
        val displayName =
          aJson.optString("displayName", context.getString(com.example.R.string.default_attachment_name))
        val mimeType = aJson.optString("mimeType", "application/octet-stream")
        val sizeBytes = aJson.optLong("sizeBytes", 0L)
        val base64 = aJson.optString("base64", "")
        if (base64.isNotBlank()) {
          val bytes = Base64.decode(base64, Base64.DEFAULT)
          val path = AttachmentStorage.writeBytesToAttachments(context, displayName, bytes)
          attachments.add(
            TaskAttachmentEntity(
              taskId = 0L,
              displayName = displayName,
              mimeType = mimeType,
              localFilePath = path,
              sizeBytes = if (sizeBytes > 0L) sizeBytes else bytes.size.toLong(),
            )
          )
          totalAttachments++
        }
      }

      tasks.add(task to attachments)
    }

    val shoppingFolders = mutableListOf<ShoppingFolderEntity>()
    val foldersArray = root.optJSONArray("shoppingFolders") ?: JSONArray()
    for (i in 0 until foldersArray.length()) {
      val fJson = foldersArray.getJSONObject(i)
      shoppingFolders.add(
        ShoppingFolderEntity(
          id = fJson.optLong("id", 0L),
          name = fJson.optString("name", ""),
          createdAtMillis = fJson.optLong("createdAtMillis", System.currentTimeMillis()),
        )
      )
    }

    val shoppingItems = mutableListOf<ShoppingItemEntity>()
    val shopArray = root.optJSONArray("shoppingItems") ?: JSONArray()
    for (i in 0 until shopArray.length()) {
      val sJson = shopArray.getJSONObject(i)
      val folderId =
        if (sJson.isNull("folderId") || !sJson.has("folderId")) null else sJson.optLong("folderId")
      shoppingItems.add(
        ShoppingItemEntity(
          folderId = folderId?.takeIf { it > 0L },
          name = sJson.optString("name", ""),
          quantity = sJson.optInt("quantity", 1).coerceAtLeast(1),
          isChecked = sJson.optBoolean("isChecked", false),
          createdAtMillis = sJson.optLong("createdAtMillis", System.currentTimeMillis()),
        )
      )
    }

    val expenseCategories = mutableListOf<ExpenseCategoryEntity>()
    val categoriesArray = root.optJSONArray("expenseCategories") ?: JSONArray()
    for (i in 0 until categoriesArray.length()) {
      val cJson = categoriesArray.getJSONObject(i)
      expenseCategories.add(
        ExpenseCategoryEntity(
          id = cJson.optLong("id", 0L),
          name = cJson.optString("name", ""),
          colorArgb = cJson.optLong("colorArgb", 0xFF006D77),
          iconKey = cJson.optString("iconKey", "category"),
          createdAtMillis = cJson.optLong("createdAtMillis", System.currentTimeMillis()),
        )
      )
    }

    val expenses = mutableListOf<ExpenseEntity>()
    val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
    for (i in 0 until expensesArray.length()) {
      val eJson = expensesArray.getJSONObject(i)
      val categoryId =
        if (eJson.isNull("categoryId") || !eJson.has("categoryId")) null
        else eJson.optLong("categoryId")
      val isInstallment = eJson.optBoolean("isInstallment", false)
      val recurrence =
        runCatching { RecurrenceType.valueOf(eJson.optString("recurrence")) }
          .getOrDefault(RecurrenceType.NONE)
      val inferredDefaultMethod =
        when {
          isInstallment -> ExpensePaymentMethod.CREDIT_INSTALLMENT
          recurrence != RecurrenceType.NONE -> ExpensePaymentMethod.RECURRING
          else -> ExpensePaymentMethod.DEBIT_ONE_TIME
        }
      val paymentMethod =
        if (eJson.has("paymentMethod") && !eJson.isNull("paymentMethod")) {
          runCatching { ExpensePaymentMethod.valueOf(eJson.optString("paymentMethod")) }
            .getOrDefault(inferredDefaultMethod)
        } else {
          inferredDefaultMethod
        }
      expenses.add(
        ExpenseEntity(
          id = eJson.optLong("id", 0L),
          title = eJson.optString("title", ""),
          notes = eJson.optString("notes", ""),
          totalAmountCents = eJson.optLong("totalAmountCents", 0L).coerceAtLeast(0L),
          categoryId = categoryId?.takeIf { it > 0L },
          isInstallment = isInstallment,
          installmentCount = eJson.optInt("installmentCount", 1).coerceAtLeast(1),
          startYear = eJson.optInt("startYear", 2026),
          startMonthZeroBased = eJson.optInt("startMonthZeroBased", 0).coerceIn(0, 11),
          dayOfMonth = eJson.optInt("dayOfMonth", 1).coerceIn(1, 31),
          dateMillis = eJson.optLong("dateMillis", System.currentTimeMillis()),
          recurrence = recurrence,
          paymentMethod = paymentMethod,
          createdAtMillis = eJson.optLong("createdAtMillis", System.currentTimeMillis()),
        )
      )
    }

    val expensePayments = mutableListOf<ExpensePaymentEntity>()
    val paymentsArray = root.optJSONArray("expensePayments") ?: JSONArray()
    for (i in 0 until paymentsArray.length()) {
      val pJson = paymentsArray.getJSONObject(i)
      val expId = pJson.optLong("expenseId", 0L)
      val key = pJson.optString("occurrenceKey", "")
      if (expId > 0L && key.isNotBlank()) {
        expensePayments.add(
          ExpensePaymentEntity(
            expenseId = expId,
            occurrenceKey = key,
            paidAtMillis = pJson.optLong("paidAtMillis", System.currentTimeMillis()),
          )
        )
      }
    }

    BackupPreview(
      exportedAtMillis = exportedAtMillis,
      taskCount = tasks.size,
      attachmentCount = totalAttachments,
      shoppingItemCount = shoppingItems.size,
      folderCount = shoppingFolders.size,
      expenseCount = expenses.size,
      payload =
        BackupPayload(
          exportedAtMillis = exportedAtMillis,
          preferences = prefs,
          tasks = tasks,
          shoppingItems = shoppingItems,
          shoppingFolders = shoppingFolders,
          expenseCategories = expenseCategories,
          expenses = expenses,
          expensePayments = expensePayments,
        ),
    )
  }
}
