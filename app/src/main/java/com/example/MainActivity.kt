package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.ExpenseEditorScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.RescheduleTaskScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.TaskEditorScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.OtterTheme
import com.example.ui.viewmodel.MainViewModel

object Routes {
  const val HOME = "home"
  const val TASK_EDITOR = "task_editor?taskId={taskId}"
  const val EXPENSE_EDITOR = "expense_editor?expenseId={expenseId}"
  const val RESCHEDULE = "reschedule/{taskId}"
  const val SETTINGS = "settings"

  fun taskEditor(taskId: Long? = null): String =
    if (taskId != null && taskId > 0L) "task_editor?taskId=$taskId" else "task_editor?taskId=-1"

  fun expenseEditor(expenseId: Long? = null): String =
    if (expenseId != null && expenseId > 0L) "expense_editor?expenseId=$expenseId"
    else "expense_editor?expenseId=-1"

  fun reschedule(taskId: Long): String = "reschedule/$taskId"
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { OtterRoot() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtterRoot(viewModel: MainViewModel = viewModel()) {
  val themePrefs by viewModel.themePreferences.collectAsStateWithLifecycle()
  val tasksState by viewModel.tasksUiState.collectAsStateWithLifecycle()
  val shoppingState by viewModel.shoppingUiState.collectAsStateWithLifecycle()
  val expensesState by viewModel.expensesUiState.collectAsStateWithLifecycle()
  val allExpenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
  val backupPreview by viewModel.backupPreview.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusMessage) {
    statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  OtterTheme(
    themeMode = themePrefs.themeMode,
    useDynamicColor = themePrefs.useDynamicColor,
    palette = themePrefs.palette,
  ) {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
      composable(Routes.HOME) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val useNavRail = maxWidth >= 600.dp

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
              TopAppBar(
                title = {
                  Text(
                    text =
                      stringResource(
                        when (selectedTab) {
                          0 -> R.string.topbar_tasks
                          1 -> R.string.topbar_shopping
                          else -> R.string.topbar_expenses
                        }
                      ),
                    style = MaterialTheme.typography.titleLarge,
                  )
                },
                actions = {
                  IconButton(
                    onClick = { navController.navigate(Routes.SETTINGS) },
                    modifier = Modifier.testTag("settings_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Settings,
                      contentDescription = stringResource(R.string.nav_settings),
                    )
                  }
                },
                colors =
                  TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                  ),
              )
            },
            bottomBar = {
              if (!useNavRail) {
                NavigationBar {
                  NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                      Icon(
                        imageVector =
                          if (selectedTab == 0) Icons.Rounded.EventAvailable
                          else Icons.Outlined.EventAvailable,
                        contentDescription = stringResource(R.string.nav_tasks),
                      )
                    },
                    label = { Text(stringResource(R.string.nav_tasks)) },
                    modifier = Modifier.testTag("tab_tasks"),
                  )
                  NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                      Icon(
                        imageVector =
                          if (selectedTab == 1) Icons.Rounded.ShoppingCart
                          else Icons.Outlined.ShoppingCart,
                        contentDescription = stringResource(R.string.nav_shopping),
                      )
                    },
                    label = { Text(stringResource(R.string.nav_shopping)) },
                    modifier = Modifier.testTag("tab_shopping"),
                  )
                  NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                      Icon(
                        imageVector =
                          if (selectedTab == 2) Icons.Rounded.AccountBalanceWallet
                          else Icons.Outlined.AccountBalanceWallet,
                        contentDescription = stringResource(R.string.nav_expenses),
                      )
                    },
                    label = { Text(stringResource(R.string.nav_expenses)) },
                    modifier = Modifier.testTag("tab_expenses"),
                  )
                }
              }
            },
            floatingActionButton = {
              when (selectedTab) {
                0 -> {
                  ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.taskEditor(null)) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_new_task)) },
                    modifier = Modifier.testTag("fab_new_task"),
                  )
                }
                2 -> {
                  ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.expenseEditor(null)) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_new_expense)) },
                    modifier = Modifier.testTag("fab_new_expense"),
                  )
                }
              }
            },
          ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              if (useNavRail) {
                NavigationRail {
                  NavigationRailItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                      Icon(
                        imageVector =
                          if (selectedTab == 0) Icons.Rounded.EventAvailable
                          else Icons.Outlined.EventAvailable,
                        contentDescription = stringResource(R.string.nav_tasks),
                      )
                    },
                    label = { Text(stringResource(R.string.nav_tasks)) },
                    modifier = Modifier.testTag("rail_tab_tasks"),
                  )
                  NavigationRailItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                      Icon(
                        imageVector =
                          if (selectedTab == 1) Icons.Rounded.ShoppingCart
                          else Icons.Outlined.ShoppingCart,
                        contentDescription = stringResource(R.string.nav_shopping),
                      )
                    },
                    label = { Text(stringResource(R.string.nav_shopping)) },
                    modifier = Modifier.testTag("rail_tab_shopping"),
                  )
                  NavigationRailItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                      Icon(
                        imageVector =
                          if (selectedTab == 2) Icons.Rounded.AccountBalanceWallet
                          else Icons.Outlined.AccountBalanceWallet,
                        contentDescription = stringResource(R.string.nav_expenses),
                      )
                    },
                    label = { Text(stringResource(R.string.nav_expenses)) },
                    modifier = Modifier.testTag("rail_tab_expenses"),
                  )
                }
              }

              when (selectedTab) {
                0 -> {
                  TasksScreen(
                    state = tasksState,
                    onSelectDay = viewModel::selectDay,
                    onChangeMonth = viewModel::changeMonth,
                    onToggleCalendar = viewModel::toggleCalendarExpanded,
                    onJumpToday = viewModel::jumpToToday,
                    onFilterChange = viewModel::setTaskFilter,
                    onCompleteTask = viewModel::completeTask,
                    onRescheduleTask = { id -> navController.navigate(Routes.reschedule(id)) },
                    onEditTask = { id -> navController.navigate(Routes.taskEditor(id)) },
                    onDeleteTask = viewModel::deleteTask,
                    onCreateTaskForDay = { navController.navigate(Routes.taskEditor(null)) },
                  )
                }
                1 -> {
                  ShoppingScreen(
                    state = shoppingState,
                    themeMode = themePrefs.themeMode,
                    onSelectFolder = viewModel::selectShoppingFolder,
                    onCreateFolder = viewModel::createShoppingFolder,
                    onRenameFolder = viewModel::renameShoppingFolder,
                    onDeleteFolder = viewModel::deleteShoppingFolder,
                    onAddItem = { name, qty -> viewModel.addShoppingItem(name, qty) },
                    onUpdateItem = viewModel::updateShoppingItem,
                    onDeleteItem = viewModel::deleteShoppingItem,
                    onClearChecked = viewModel::clearCheckedShoppingItems,
                  )
                }
                else -> {
                  ExpensesScreen(
                    state = expensesState,
                    onSelectDay = viewModel::selectExpenseDay,
                    onChangeMonth = viewModel::changeExpenseMonth,
                    onToggleCalendar = viewModel::toggleExpenseCalendarExpanded,
                    onJumpToday = viewModel::jumpExpenseToday,
                    onFilterChange = viewModel::setExpenseFilter,
                    onCategoryFilterChange = viewModel::setExpenseCategoryFilter,
                    onCreateCategory = { name, colorArgb, iconKey ->
                      viewModel.createExpenseCategory(name, colorArgb, iconKey)
                    },
                    onDeleteCategory = viewModel::deleteExpenseCategory,
                    onTogglePaid = viewModel::toggleOccurrencePaid,
                    onEditExpense = { id -> navController.navigate(Routes.expenseEditor(id)) },
                    onDeleteExpense = viewModel::deleteExpense,
                    onCreateExpense = { navController.navigate(Routes.expenseEditor(null)) },
                  )
                }
              }
            }
          }
        }
      }

      composable(
        route = Routes.TASK_EDITOR,
        arguments =
          listOf(
            navArgument("taskId") {
              type = NavType.LongType
              defaultValue = -1L
            }
          ),
      ) { entry ->
        val taskIdArg = entry.arguments?.getLong("taskId")?.takeIf { it > 0L }
        TaskEditorScreen(
          taskId = taskIdArg,
          initialDay = tasksState.selectedDay,
          loadTask = viewModel::getTaskById,
          onSave = { task, attachments ->
            viewModel.saveTask(task, attachments) { navController.popBackStack() }
          },
          onNavigateBack = { navController.popBackStack() },
        )
      }

      composable(
        route = Routes.EXPENSE_EDITOR,
        arguments =
          listOf(
            navArgument("expenseId") {
              type = NavType.LongType
              defaultValue = -1L
            }
          ),
      ) { entry ->
        val expenseIdArg = entry.arguments?.getLong("expenseId")?.takeIf { it > 0L }
        ExpenseEditorScreen(
          expenseId = expenseIdArg,
          initialDay = expensesState.selectedDay,
          categories = allExpenseCategories,
          loadExpense = viewModel::getExpenseById,
          onCreateCategory = viewModel::createExpenseCategory,
          onSave = { expense -> viewModel.saveExpense(expense) { navController.popBackStack() } },
          onNavigateBack = { navController.popBackStack() },
        )
      }

      composable(
        route = Routes.RESCHEDULE,
        arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
      ) { entry ->
        val taskIdArg = entry.arguments?.getLong("taskId") ?: -1L
        RescheduleTaskScreen(
          taskId = taskIdArg,
          loadTask = viewModel::getTaskById,
          onConfirmReschedule = { id, newDue ->
            viewModel.rescheduleTask(id, newDue) { navController.popBackStack() }
          },
          onNavigateBack = { navController.popBackStack() },
        )
      }

      composable(Routes.SETTINGS) {
        SettingsScreen(
          themePreferences = themePrefs,
          backupPreview = backupPreview,
          onSetThemeMode = viewModel::setThemeMode,
          onSetDynamicColor = viewModel::setUseDynamicColor,
          onSetPalette = viewModel::setPalette,
          onExportBackup = viewModel::exportBackup,
          onLoadBackupPreview = viewModel::loadBackupPreview,
          onDismissBackupPreview = viewModel::dismissBackupPreview,
          onConfirmImport = viewModel::applyBackupImport,
          onNavigateBack = { navController.popBackStack() },
        )
      }
    }
  }
}
