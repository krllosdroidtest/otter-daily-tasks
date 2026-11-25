package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.ShoppingFolderEntity
import com.example.data.model.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(private val database: AppDatabase) {
  private val shoppingDao = database.shoppingDao()

  val allFolders: Flow<List<ShoppingFolderEntity>> = shoppingDao.observeAllFolders()

  val allItems: Flow<List<ShoppingItemEntity>> = shoppingDao.observeAllItems()

  suspend fun getAllFoldersSnapshot(): List<ShoppingFolderEntity> =
    shoppingDao.getAllFoldersSnapshot()

  suspend fun getAllItemsSnapshot(): List<ShoppingItemEntity> =
    shoppingDao.getAllItemsSnapshot()

  suspend fun createFolder(name: String): Long =
    shoppingDao.insertFolder(ShoppingFolderEntity(name = name.trim()))

  suspend fun renameFolder(folder: ShoppingFolderEntity, newName: String) {
    shoppingDao.updateFolder(folder.copy(name = newName.trim()))
  }

  suspend fun deleteFolder(folder: ShoppingFolderEntity) = shoppingDao.deleteFolder(folder)

  suspend fun addItem(name: String = "", quantity: Int = 1, folderId: Long? = null): Long =
    shoppingDao.insertItem(
      ShoppingItemEntity(
        folderId = folderId,
        name = name,
        quantity = quantity.coerceAtLeast(1),
      )
    )

  suspend fun updateItem(item: ShoppingItemEntity) = shoppingDao.updateItem(item)

  suspend fun deleteItem(item: ShoppingItemEntity) = shoppingDao.deleteItem(item)

  suspend fun clearChecked(folderId: Long? = null) {
    if (folderId == null) {
      shoppingDao.deleteCheckedItemsInGeneral()
    } else {
      shoppingDao.deleteCheckedItemsInFolder(folderId)
    }
  }

  suspend fun replaceAllShoppingData(
    folders: List<ShoppingFolderEntity>,
    items: List<ShoppingItemEntity>,
  ) =
    database.withTransaction {
      shoppingDao.deleteAllItems()
      shoppingDao.deleteAllFolders()
      insertFoldersAndItems(folders, items)
    }

  suspend fun insertAdditionalShoppingData(
    folders: List<ShoppingFolderEntity>,
    items: List<ShoppingItemEntity>,
  ) =
    database.withTransaction {
      insertFoldersAndItems(folders, items)
    }

  private suspend fun insertFoldersAndItems(
    folders: List<ShoppingFolderEntity>,
    items: List<ShoppingItemEntity>,
  ) {
    val folderIdMap = mutableMapOf<Long, Long>()
    for (folder in folders) {
      val newId = shoppingDao.insertFolder(folder.copy(id = 0L))
      if (folder.id > 0L) {
        folderIdMap[folder.id] = newId
      }
    }
    for (item in items) {
      val mappedFolderId = item.folderId?.let { oldId -> folderIdMap[oldId] }
      shoppingDao.insertItem(item.copy(id = 0L, folderId = mappedFolderId))
    }
  }
}
