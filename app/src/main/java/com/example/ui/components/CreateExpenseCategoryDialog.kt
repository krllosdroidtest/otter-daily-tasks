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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseCategoryStyleCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateExpenseCategoryDialog(
  existingCategories: List<ExpenseCategoryEntity> = emptyList(),
  onSelectExisting: ((Long) -> Unit)? = null,
  onDismiss: () -> Unit,
  onCreate: (name: String, colorArgb: Long, iconKey: String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var selectedColorArgb by remember {
    mutableLongStateOf(ExpenseCategoryStyleCatalog.colors.first().argb)
  }
  var selectedIconKey by remember {
    mutableStateOf(ExpenseCategoryStyleCatalog.icons.first().key)
  }
  var nameError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.expense_category_create_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (existingCategories.isNotEmpty()) {
          Text(
            text = stringResource(R.string.expense_category_history_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          if (onSelectExisting != null) {
            Text(
              text = stringResource(R.string.expense_category_select_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            existingCategories.forEach { cat ->
              val catColor = Color(cat.colorArgb.toInt())
              AssistChip(
                onClick = {
                  if (onSelectExisting != null) {
                    onSelectExisting(cat.id)
                  } else {
                    name = cat.name
                    selectedColorArgb = cat.colorArgb
                    selectedIconKey = cat.iconKey
                    nameError = false
                  }
                },
                leadingIcon = {
                  Icon(
                    imageVector = ExpenseCategoryStyleCatalog.iconFor(cat.iconKey),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text(cat.name) },
                modifier = Modifier.testTag("history_category_${cat.id}"),
              )
            }
          }
          HorizontalDivider()
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            nameError = false
          },
          label = { Text(stringResource(R.string.expense_category_name_label)) },
          placeholder = { Text(stringResource(R.string.expense_category_name_placeholder)) },
          singleLine = true,
          isError = nameError,
          supportingText =
            if (nameError) ({ Text(stringResource(R.string.expense_category_name_error)) })
            else null,
          modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
        )

        Text(
          text = stringResource(R.string.expense_category_color_label),
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
                  .testTag("category_color_$idx"),
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

        Text(
          text = stringResource(R.string.expense_category_icon_label),
          style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ExpenseCategoryStyleCatalog.icons.forEach { iconOption ->
            val isSelected = selectedIconKey == iconOption.key
            val tint = Color(selectedColorArgb.toInt())
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (isSelected) tint.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
              border = if (isSelected) BorderStroke(2.dp, tint) else null,
              modifier =
                Modifier.size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { selectedIconKey = iconOption.key }
                  .testTag("category_icon_${iconOption.key}"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = iconOption.icon,
                  contentDescription = iconOption.key,
                  tint = if (isSelected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }
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
            onCreate(clean, selectedColorArgb, selectedIconKey)
          }
        },
        modifier = Modifier.testTag("category_save_button"),
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
