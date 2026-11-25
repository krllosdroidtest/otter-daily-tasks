package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShoppingFolderEntity
import com.example.data.model.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
  @Query("SELECT * FROM shopping_folders ORDER BY createdAtMillis ASC, id ASC")
  fun observeAllFolders(): Flow<List<ShoppingFolderEntity>>

  @Query("SELECT * FROM shopping_folders ORDER BY createdAtMillis ASC, id ASC")
  suspend fun getAllFoldersSnapshot(): List<ShoppingFolderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolder(folder: ShoppingFolderEntity): Long

  @Update
  suspend fun updateFolder(folder: ShoppingFolderEntity)

  @Delete
  suspend fun deleteFolder(folder: ShoppingFolderEntity)

  @Query("DELETE FROM shopping_folders")
  suspend fun deleteAllFolders()

  @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, createdAtMillis ASC")
  fun observeAllItems(): Flow<List<ShoppingItemEntity>>

  @Query("SELECT * FROM shopping_items ORDER BY createdAtMillis ASC")
  suspend fun getAllItemsSnapshot(): List<ShoppingItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItem(item: ShoppingItemEntity): Long

  @Update
  suspend fun updateItem(item: ShoppingItemEntity)

  @Delete
  suspend fun deleteItem(item: ShoppingItemEntity)

  @Query("DELETE FROM shopping_items WHERE isChecked = 1 AND folderId IS NULL")
  suspend fun deleteCheckedItemsInGeneral()

  @Query("DELETE FROM shopping_items WHERE isChecked = 1 AND folderId = :folderId")
  suspend fun deleteCheckedItemsInFolder(folderId: Long)

  @Query("DELETE FROM shopping_items WHERE isChecked = 1")
  suspend fun deleteCheckedItems()

  @Query("DELETE FROM shopping_items")
  suspend fun deleteAllItems()
}
