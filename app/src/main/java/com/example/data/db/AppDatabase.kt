package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpensePaymentEntity
import com.example.data.model.ShoppingFolderEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.TaskAttachmentEntity
import com.example.data.model.TaskEntity

@Database(
  entities =
    [
      TaskEntity::class,
      TaskAttachmentEntity::class,
      ShoppingFolderEntity::class,
      ShoppingItemEntity::class,
      ExpenseCategoryEntity::class,
      CreditCardEntity::class,
      ExpenseEntity::class,
      ExpensePaymentEntity::class,
    ],
  version = 5,
  exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun taskDao(): TaskDao

  abstract fun shoppingDao(): ShoppingDao

  abstract fun expenseDao(): ExpenseDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_folders` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `createdAtMillis` INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_items_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `folderId` INTEGER,
              `name` TEXT NOT NULL,
              `quantity` INTEGER NOT NULL,
              `isChecked` INTEGER NOT NULL,
              `createdAtMillis` INTEGER NOT NULL,
              FOREIGN KEY(`folderId`) REFERENCES `shopping_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO `shopping_items_new` (`id`, `folderId`, `name`, `quantity`, `isChecked`, `createdAtMillis`)
            SELECT `id`, NULL, `name`, `quantity`, `isChecked`, `createdAtMillis` FROM `shopping_items`
            """.trimIndent()
          )
          db.execSQL("DROP TABLE `shopping_items`")
          db.execSQL("ALTER TABLE `shopping_items_new` RENAME TO `shopping_items`")
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_shopping_items_folderId` ON `shopping_items` (`folderId`)"
          )

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expense_categories` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `colorArgb` INTEGER NOT NULL,
              `iconKey` TEXT NOT NULL,
              `createdAtMillis` INTEGER NOT NULL
            )
            """.trimIndent()
          )

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `title` TEXT NOT NULL,
              `notes` TEXT NOT NULL,
              `totalAmountCents` INTEGER NOT NULL,
              `categoryId` INTEGER,
              `isInstallment` INTEGER NOT NULL,
              `installmentCount` INTEGER NOT NULL,
              `startYear` INTEGER NOT NULL,
              `startMonthZeroBased` INTEGER NOT NULL,
              `dayOfMonth` INTEGER NOT NULL,
              `dateMillis` INTEGER NOT NULL,
              `createdAtMillis` INTEGER NOT NULL,
              FOREIGN KEY(`categoryId`) REFERENCES `expense_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)"
          )
        }
      }

    val MIGRATION_2_3: Migration =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE `expenses` ADD COLUMN `recurrence` TEXT NOT NULL DEFAULT 'NONE'"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expense_payments` (
              `expenseId` INTEGER NOT NULL,
              `occurrenceKey` TEXT NOT NULL,
              `paidAtMillis` INTEGER NOT NULL,
              PRIMARY KEY(`expenseId`, `occurrenceKey`),
              FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expense_payments_expenseId` ON `expense_payments` (`expenseId`)"
          )
        }
      }

    val MIGRATION_3_4: Migration =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE `expenses` ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT 'DEBIT_ONE_TIME'"
          )
          db.execSQL(
            "UPDATE `expenses` SET `paymentMethod` = 'CREDIT_INSTALLMENT' WHERE `isInstallment` = 1"
          )
          db.execSQL(
            "UPDATE `expenses` SET `paymentMethod` = 'RECURRING' WHERE `isInstallment` = 0 AND `recurrence` != 'NONE'"
          )
        }
      }

    val MIGRATION_4_5: Migration =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `credit_cards` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `colorArgb` INTEGER NOT NULL,
              `closingDay` INTEGER NOT NULL,
              `dueDay` INTEGER NOT NULL,
              `createdAtMillis` INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `title` TEXT NOT NULL,
              `notes` TEXT NOT NULL,
              `totalAmountCents` INTEGER NOT NULL,
              `categoryId` INTEGER,
              `isInstallment` INTEGER NOT NULL,
              `installmentCount` INTEGER NOT NULL,
              `startYear` INTEGER NOT NULL,
              `startMonthZeroBased` INTEGER NOT NULL,
              `dayOfMonth` INTEGER NOT NULL,
              `dateMillis` INTEGER NOT NULL,
              `recurrence` TEXT NOT NULL,
              `paymentMethod` TEXT NOT NULL,
              `creditCardId` INTEGER,
              `createdAtMillis` INTEGER NOT NULL,
              FOREIGN KEY(`categoryId`) REFERENCES `expense_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
              FOREIGN KEY(`creditCardId`) REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO `expenses_new` (
              `id`, `title`, `notes`, `totalAmountCents`, `categoryId`, `isInstallment`,
              `installmentCount`, `startYear`, `startMonthZeroBased`, `dayOfMonth`,
              `dateMillis`, `recurrence`, `paymentMethod`, `creditCardId`, `createdAtMillis`
            )
            SELECT
              `id`, `title`, `notes`, `totalAmountCents`, `categoryId`, `isInstallment`,
              `installmentCount`, `startYear`, `startMonthZeroBased`, `dayOfMonth`,
              `dateMillis`, `recurrence`, `paymentMethod`, NULL, `createdAtMillis`
            FROM `expenses`
            """.trimIndent()
          )
          db.execSQL("DROP TABLE `expenses`")
          db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_creditCardId` ON `expenses` (`creditCardId`)"
          )
        }
      }

    fun getInstance(context: Context): AppDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "otter_offline.db",
              )
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
              .build()
              .also { INSTANCE = it }
        }
  }
}
