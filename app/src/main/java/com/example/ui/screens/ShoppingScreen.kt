package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ShoppingFolderEntity
import com.example.data.model.ShoppingItemEntity
import com.example.ui.theme.CheckedShoppingRedDark
import com.example.ui.theme.CheckedShoppingRedLight
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.ShoppingUiState

@Composable
fun ShoppingScreen(
  state: ShoppingUiState,
  themeMode: ThemeMode,
  onSelectFolder: (Long?) -> Unit,
  onCreateFolder: (String) -> Unit,
  onRenameFolder: (ShoppingFolderEntity, String) -> Unit,
  onDeleteFolder: (ShoppingFolderEntity) -> Unit,
  onAddItem: (String, Int) -> Unit,
  onUpdateItem: (ShoppingItemEntity) -> Unit,
  onDeleteItem: (ShoppingItemEntity) -> Unit,
  onClearChecked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var newItemText by remember { mutableStateOf("") }
  var showCreateFolderDialog by remember { mutableStateOf(false) }
  var folderPendingRename by remember { mutableStateOf<ShoppingFolderEntity?>(null) }
  var folderPendingDelete by remember { mutableStateOf<ShoppingFolderEntity?>(null) }

  val items = state.visibleItems
  val activeFolder = state.folders.firstOrNull { it.id == state.selectedFolderId }

  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
    }
  val checkedRed = if (isDark) CheckedShoppingRedDark else CheckedShoppingRedLight
  val checkedCount = items.count { it.isChecked }

  fun submitQuickNewItem() {
    val clean = newItemText.trim()
    if (clean.isNotEmpty()) {
      onAddItem(clean, 1)
      newItemText = ""
    } else {
      onAddItem("", 1)
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Shopping folders selector & actions
      item(key = "shopping_folders") {
        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val generalCount = state.itemCountByFolder[null] ?: 0
              FilterChip(
                selected = state.selectedFolderId == null,
                onClick = { onSelectFolder(null) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = {
                  Text("${stringResource(R.string.shopping_folder_general)} ($generalCount)")
                },
                modifier = Modifier.testTag("shopping_folder_general"),
              )

              state.folders.forEach { folder ->
                val count = state.itemCountByFolder[folder.id] ?: 0
                FilterChip(
                  selected = state.selectedFolderId == folder.id,
                  onClick = { onSelectFolder(folder.id) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Rounded.Folder,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                  },
                  label = {
                    Text(
                      text = "${folder.name} ($count)",
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                  modifier = Modifier.testTag("shopping_folder_chip_${folder.id}"),
                )
              }

              AssistChip(
                onClick = { showCreateFolderDialog = true },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = { Text(stringResource(R.string.shopping_folder_new)) },
                modifier = Modifier.testTag("shopping_new_folder_button"),
              )
            }

            if (activeFolder != null) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = activeFolder.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { folderPendingRename = activeFolder },
                    modifier = Modifier.testTag("shopping_rename_folder_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Edit,
                      contentDescription = stringResource(R.string.shopping_folder_rename_cd),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(
                    onClick = { folderPendingDelete = activeFolder },
                    modifier = Modifier.testTag("shopping_delete_folder_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.DeleteOutline,
                      contentDescription = stringResource(R.string.shopping_folder_delete_cd),
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Quick add bar + summary
      item(key = "shopping_controls") {
        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text(stringResource(R.string.shopping_add_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitQuickNewItem() }),
                modifier = Modifier.weight(1f).testTag("shopping_new_item_input"),
              )
              FilledTonalIconButton(
                onClick = ::submitQuickNewItem,
                modifier = Modifier.size(52.dp).testTag("shopping_add_item_button"),
              ) {
                Icon(
                  Icons.Rounded.Add,
                  contentDescription = stringResource(R.string.shopping_add_cd),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (items.isEmpty()) stringResource(R.string.shopping_empty_status)
                  else stringResource(R.string.shopping_checked_status, checkedCount, items.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (checkedCount > 0) {
                TextButton(
                  onClick = onClearChecked,
                  modifier = Modifier.testTag("shopping_clear_checked_button"),
                ) {
                  Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(stringResource(R.string.shopping_clear_checked))
                }
              }
            }
          }
        }
      }

      if (items.isNotEmpty()) {
        // Explicit 2-column table header
        item(key = "shopping_table_header") {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.shopping_col_item),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = stringResource(R.string.shopping_col_qty),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(140.dp),
              )
            }
          }
        }

        items(items = items, key = { it.id }) { item ->
          ShoppingRowCard(
            item = item,
            checkedRedColor = checkedRed,
            onUpdate = onUpdateItem,
            onDelete = { onDeleteItem(item) },
          )
        }
      } else {
        item(key = "shopping_empty_state") {
          ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors =
              CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_empty_shopping),
                contentDescription = stringResource(R.string.shopping_empty_cd),
                modifier =
                  Modifier.fillMaxWidth(0.7f).height(150.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
              )
              Text(
                text = stringResource(R.string.shopping_empty_title),
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = stringResource(R.string.shopping_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              Button(
                onClick = { onAddItem("", 1) },
                modifier = Modifier.testTag("shopping_empty_add_button"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.shopping_add_row))
              }
            }
          }
        }
      }
    }
  }

  if (showCreateFolderDialog) {
    ShoppingFolderNameDialog(
      title = stringResource(R.string.shopping_folder_create_title),
      initialName = "",
      onDismiss = { showCreateFolderDialog = false },
      onConfirm = { name ->
        onCreateFolder(name)
        showCreateFolderDialog = false
      },
    )
  }

  folderPendingRename?.let { folder ->
    ShoppingFolderNameDialog(
      title = stringResource(R.string.shopping_folder_rename_title),
      initialName = folder.name,
      onDismiss = { folderPendingRename = null },
      onConfirm = { newName ->
        onRenameFolder(folder, newName)
        folderPendingRename = null
      },
    )
  }

  folderPendingDelete?.let { folder ->
    val count = state.itemCountByFolder[folder.id] ?: 0
    AlertDialog(
      onDismissRequest = { folderPendingDelete = null },
      title = { Text(stringResource(R.string.shopping_folder_delete_title)) },
      text = {
        Text(stringResource(R.string.shopping_folder_delete_message, folder.name, count))
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteFolder(folder)
            folderPendingDelete = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.testTag("shopping_confirm_delete_folder"),
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { folderPendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun ShoppingFolderNameDialog(
  title: String,
  initialName: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember(initialName) { mutableStateOf(initialName) }
  var showError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          showError = false
        },
        label = { Text(stringResource(R.string.shopping_folder_name_label)) },
        placeholder = { Text(stringResource(R.string.shopping_folder_name_placeholder)) },
        singleLine = true,
        isError = showError,
        supportingText =
          if (showError) ({ Text(stringResource(R.string.shopping_folder_name_error)) }) else null,
        modifier = Modifier.fillMaxWidth().testTag("shopping_folder_name_input"),
      )
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = name.trim()
          if (clean.isEmpty()) {
            showError = true
          } else {
            onConfirm(clean)
          }
        },
        modifier = Modifier.testTag("shopping_folder_save_button"),
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

@Composable
private fun ShoppingRowCard(
  item: ShoppingItemEntity,
  checkedRedColor: Color,
  onUpdate: (ShoppingItemEntity) -> Unit,
  onDelete: () -> Unit,
) {
  var localName by remember(item.id) { mutableStateOf(item.name) }
  LaunchedEffect(item.name) {
    if (localName != item.name) {
      localName = item.name
    }
  }

  val textStyle =
    MaterialTheme.typography.bodyLarge.copy(
      color = if (item.isChecked) checkedRedColor else MaterialTheme.colorScheme.onSurface,
      textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
      fontWeight = if (item.isChecked) FontWeight.Medium else FontWeight.Normal,
    )

  ElevatedCard(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor =
          if (item.isChecked) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
          else MaterialTheme.colorScheme.surfaceContainerLow
      ),
    modifier = Modifier.fillMaxWidth().testTag("shopping_row_${item.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Column 1: Checkbox + Editable written item text
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = item.isChecked,
          onCheckedChange = { checked -> onUpdate(item.copy(isChecked = checked)) },
          colors =
            CheckboxDefaults.colors(
              checkedColor = checkedRedColor,
              checkmarkColor = MaterialTheme.colorScheme.surface,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("shopping_check_${item.id}"),
        )

        Box(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
          if (localName.isEmpty()) {
            Text(
              text = stringResource(R.string.shopping_row_placeholder),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
          }
          BasicTextField(
            value = localName,
            onValueChange = { updatedText ->
              localName = updatedText
              onUpdate(item.copy(name = updatedText))
            },
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("shopping_text_${item.id}"),
          )
        }
      }

      // Vertical separator between columns
      Box(
        modifier =
          Modifier.width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
      )

      Spacer(Modifier.width(6.dp))

      // Column 2: Quantity selection with "-" and "+" buttons + remove button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = {
                if (item.quantity > 1) {
                  onUpdate(item.copy(quantity = item.quantity - 1))
                }
              },
              enabled = item.quantity > 1,
              modifier = Modifier.size(40.dp).testTag("shopping_minus_${item.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.shopping_qty_minus_cd),
                modifier = Modifier.size(18.dp),
              )
            }

            Text(
              text = item.quantity.toString(),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              modifier = Modifier.width(24.dp).testTag("shopping_qty_${item.id}"),
            )

            IconButton(
              onClick = { onUpdate(item.copy(quantity = item.quantity + 1)) },
              modifier = Modifier.size(40.dp).testTag("shopping_plus_${item.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.shopping_qty_plus_cd),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(40.dp).testTag("shopping_delete_${item.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.shopping_delete_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
  }
}
