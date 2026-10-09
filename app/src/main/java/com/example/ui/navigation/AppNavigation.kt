package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.InvoiceWithItems
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.bank.BankAccountListScreen
import com.example.ui.screens.customer.CustomerDetailScreen
import com.example.ui.screens.customer.CustomerListScreen
import com.example.ui.screens.invoice.InvoiceCreateEditScreen
import com.example.ui.screens.invoice.InvoiceListScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.viewmodel.BankAccountViewModel
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.ui.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Main : Screen("main", "اصلی", Icons.Filled.Home, Icons.Outlined.Home)
    object Home : Screen("home", "خانه", Icons.Filled.Home, Icons.Outlined.Home)
    object Invoices : Screen("invoices", "فاکتورها", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Customers : Screen("customers", "مشتریان", Icons.Filled.People, Icons.Outlined.People)
    object BankAccounts : Screen("bank_accounts", "حساب‌ها", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    object Settings : Screen("settings", "تنظیمات", Icons.Filled.Settings, Icons.Outlined.Settings)
    object InvoiceCreateEdit : Screen("invoice_create_edit", "صدور فاکتور", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object CustomerDetail : Screen("customer_detail/{customerId}", "جزئیات مشتری", Icons.Filled.People, Icons.Outlined.People) {
        fun createRoute(customerId: Long) = "customer_detail/$customerId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Invoices,
    Screen.Customers,
    Screen.BankAccounts,
    Screen.Settings
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    invoiceViewModel: InvoiceViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel(),
    bankAccountViewModel: BankAccountViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(Screen.Home.route) }

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(180))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(140))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(180))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(140))
        }
    ) {
        // Main Screen hosting the 5 tabs with the persistent bottom bar
        composable(Screen.Main.route) {
            BackHandler(enabled = currentTab != Screen.Home.route) {
                currentTab = Screen.Home.route
            }

            MainTabsScreen(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                invoiceViewModel = invoiceViewModel,
                customerViewModel = customerViewModel,
                bankAccountViewModel = bankAccountViewModel,
                settingsViewModel = settingsViewModel,
                onCreateInvoiceClick = {
                    val settings = settingsViewModel.settings.value
                    invoiceViewModel.initNewInvoice(settings)
                    navController.navigate(Screen.InvoiceCreateEdit.route)
                },
                onEditInvoiceClick = { invoiceWithItems ->
                    invoiceViewModel.loadInvoiceForEdit(invoiceWithItems)
                    navController.navigate(Screen.InvoiceCreateEdit.route)
                },
                onCustomerClick = { customerId ->
                    navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                }
            )
        }

        // Invoice Create / Edit (Full Screen)
        composable(Screen.InvoiceCreateEdit.route) {
            InvoiceCreateEditScreen(
                invoiceViewModel = invoiceViewModel,
                customerViewModel = customerViewModel,
                bankAccountViewModel = bankAccountViewModel,
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Customer Details & Invoices History (Full Screen)
        composable(
            route = Screen.CustomerDetail.route,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            CustomerDetailScreen(
                customerId = customerId,
                customerViewModel = customerViewModel,
                invoiceViewModel = invoiceViewModel,
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onCreateInvoiceForCustomer = { customer ->
                    val settings = settingsViewModel.settings.value
                    invoiceViewModel.initNewInvoice(settings)
                    invoiceViewModel.updateInvoiceState { inv ->
                        inv.copy(
                            customerId = customer.id,
                            customerName = customer.name,
                            customerPhone = customer.phone,
                            customerAddress = customer.address
                        )
                    }
                    navController.navigate(Screen.InvoiceCreateEdit.route)
                },
                onInvoiceClick = { invoiceWithItems ->
                    invoiceViewModel.loadInvoiceForEdit(invoiceWithItems)
                    navController.navigate(Screen.InvoiceCreateEdit.route)
                }
            )
        }
    }
}

@Composable
fun MainTabsScreen(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    invoiceViewModel: InvoiceViewModel,
    customerViewModel: CustomerViewModel,
    bankAccountViewModel: BankAccountViewModel,
    settingsViewModel: SettingsViewModel,
    onCreateInvoiceClick: () -> Unit,
    onEditInvoiceClick: (InvoiceWithItems) -> Unit,
    onCustomerClick: (Long) -> Unit
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = BrandBlue,
        selectedTextColor = BrandBlue,
        indicatorColor = BrandBlueContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentTab == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            },
                            selected = isSelected,
                            colors = navItemColors,
                            onClick = {
                                onTabSelected(screen.route)
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentTab) {
                Screen.Home.route -> {
                    HomeScreen(
                        invoiceViewModel = invoiceViewModel,
                        customerViewModel = customerViewModel,
                        settingsViewModel = settingsViewModel,
                        onCreateInvoiceClick = onCreateInvoiceClick,
                        onNavigateToInvoices = { onTabSelected(Screen.Invoices.route) },
                        onNavigateToCustomers = { onTabSelected(Screen.Customers.route) },
                        onInvoiceClick = onEditInvoiceClick
                    )
                }
                Screen.Invoices.route -> {
                    InvoiceListScreen(
                        viewModel = invoiceViewModel,
                        settingsViewModel = settingsViewModel,
                        onCreateNewInvoice = onCreateInvoiceClick,
                        onEditInvoice = onEditInvoiceClick
                    )
                }
                Screen.Customers.route -> {
                    CustomerListScreen(
                        viewModel = customerViewModel,
                        onCustomerClick = onCustomerClick
                    )
                }
                Screen.BankAccounts.route -> {
                    BankAccountListScreen(viewModel = bankAccountViewModel)
                }
                Screen.Settings.route -> {
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}

