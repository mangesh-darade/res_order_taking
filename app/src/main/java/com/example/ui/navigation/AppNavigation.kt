package com.example.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.MainTab
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterInfoScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.finalize.FinalizeScreen
import com.example.ui.screens.menu.MenuScreen
import com.example.ui.screens.orders.OrdersScreen
import com.example.ui.screens.sections.SectionsScreen
import com.example.ui.screens.tables.TablesScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val REGISTER_INFO = "register_info"

    const val SECTIONS = "sections"
    const val TABLES = "tables?secId={secId}&secName={secName}&subId={subId}&subName={subName}"
    const val ORDERS = "orders?tableId={tableId}"
    const val MENU = "menu/{tableId}/{guestId}"
    const val FINALIZE = "finalize/{orderId}"

    fun buildTablesRoute(secId: String, secName: String, subId: String? = null, subName: String? = null): String {
        val sId = subId ?: ""
        val sName = subName ?: ""
        return "tables?secId=$secId&secName=$secName&subId=$sId&subName=$sName"
    }

    fun buildOrdersRoute(tableId: String? = null): String {
        return if (tableId != null) "orders?tableId=$tableId" else "orders"
    }

    fun buildMenuRoute(tableId: String, guestId: Int): String {
        return "menu/$tableId/$guestId"
    }

    fun buildFinalizeRoute(orderId: String): String {
        return "finalize/$orderId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    var currentSectionId by remember { mutableStateOf("1") }
    var currentSectionName by remember { mutableStateOf("Main Dining") }
    var currentSubsectionId by remember { mutableStateOf<String?>("101") }
    var currentSubsectionName by remember { mutableStateOf<String?>("Hall A") }
    var currentTableId by remember { mutableStateOf<String?>("1") }

    val navigateToLogout = {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Auth: Splash
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.SECTIONS) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Auth: Login
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.SECTIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                },
                onNavigateToRegisterInfo = {
                    navController.navigate(Routes.REGISTER_INFO)
                }
            )
        }

        // Auth: Forgot Password
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Auth: Register Info
        composable(Routes.REGISTER_INFO) {
            RegisterInfoScreen(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Screen A: Sections
        composable(Routes.SECTIONS) {
            SectionsScreen(
                onNavigateToTables = { secId, secName, subId, subName ->
                    currentSectionId = secId
                    currentSectionName = secName
                    currentSubsectionId = subId
                    currentSubsectionName = subName
                    navController.navigate(Routes.buildTablesRoute(secId, secName, subId, subName))
                },
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.SECTIONS -> { /* already here */ }
                        MainTab.TABLES -> navController.navigate(Routes.buildTablesRoute(currentSectionId, currentSectionName, currentSubsectionId, currentSubsectionName))
                        MainTab.ORDERS -> navController.navigate(Routes.buildOrdersRoute(currentTableId))
                    }
                },
                onLogoutClick = navigateToLogout
            )
        }

        // Screen B: Tables
        composable(
            route = Routes.TABLES,
            arguments = listOf(
                navArgument("secId") { type = NavType.StringType; defaultValue = "1" },
                navArgument("secName") { type = NavType.StringType; defaultValue = "Main Dining" },
                navArgument("subId") { type = NavType.StringType; defaultValue = "" },
                navArgument("subName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val secId = backStackEntry.arguments?.getString("secId") ?: currentSectionId
            val secName = backStackEntry.arguments?.getString("secName") ?: currentSectionName
            val rawSubId = backStackEntry.arguments?.getString("subId") ?: ""
            val rawSubName = backStackEntry.arguments?.getString("subName") ?: ""

            val subId = rawSubId.ifEmpty { null }
            val subName = rawSubName.ifEmpty { null }

            TablesScreen(
                sectionId = secId,
                sectionName = secName,
                subsectionId = subId,
                subsectionName = subName,
                onNavigateToOrders = { tId ->
                    currentTableId = tId
                    navController.navigate(Routes.buildOrdersRoute(tId))
                },
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.SECTIONS -> navController.navigate(Routes.SECTIONS)
                        MainTab.TABLES -> { /* already here */ }
                        MainTab.ORDERS -> navController.navigate(Routes.buildOrdersRoute(currentTableId))
                    }
                },
                onLogoutClick = navigateToLogout
            )
        }

        // Screen C: Orders (Order Hub)
        composable(
            route = Routes.ORDERS,
            arguments = listOf(
                navArgument("tableId") { type = NavType.StringType; defaultValue = "1" }
            )
        ) { backStackEntry ->
            val tId = backStackEntry.arguments?.getString("tableId") ?: currentTableId ?: "1"

            OrdersScreen(
                initialTableId = tId,
                onNavigateToMenu = { tableId, guestId ->
                    navController.navigate(Routes.buildMenuRoute(tableId, guestId))
                },
                onNavigateToFinalize = { orderId ->
                    navController.navigate(Routes.buildFinalizeRoute(orderId))
                },
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.SECTIONS -> navController.navigate(Routes.SECTIONS)
                        MainTab.TABLES -> navController.navigate(Routes.buildTablesRoute(currentSectionId, currentSectionName, currentSubsectionId, currentSubsectionName))
                        MainTab.ORDERS -> { /* already here */ }
                    }
                },
                onLogoutClick = navigateToLogout
            )
        }

        // Screen D: Order Screen (Menu / take order)
        composable(
            route = Routes.MENU,
            arguments = listOf(
                navArgument("tableId") { type = NavType.StringType },
                navArgument("guestId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val tId = backStackEntry.arguments?.getString("tableId") ?: "1"
            val gId = backStackEntry.arguments?.getInt("guestId") ?: 1

            MenuScreen(
                tableId = tId,
                guestId = gId,
                onBackToOrders = {
                    // MUST return to Orders (NOT Tables)
                    navController.popBackStack()
                }
            )
        }

        // Screen E: Finalize
        composable(
            route = Routes.FINALIZE,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

            FinalizeScreen(
                orderId = orderId,
                onBackToOrder = {
                    navController.popBackStack()
                },
                onNavigateToTables = {
                    // Auto-return to Tables screen
                    navController.navigate(Routes.buildTablesRoute(currentSectionId, currentSectionName, currentSubsectionId, currentSubsectionName)) {
                        popUpTo(Routes.SECTIONS) { inclusive = false }
                    }
                }
            )
        }
    }
}

