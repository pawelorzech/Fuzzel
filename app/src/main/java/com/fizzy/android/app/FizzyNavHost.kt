package com.fizzy.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fizzy.android.feature.auth.AuthScreen
import com.fizzy.android.feature.auth.AuthViewModel
import com.fizzy.android.feature.boards.BoardListScreen
import com.fizzy.android.feature.card.CardDetailScreen
import com.fizzy.android.feature.kanban.KanbanScreen
import com.fizzy.android.feature.notifications.NotificationsScreen
import com.fizzy.android.feature.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Boards : Screen("boards")
    data object Kanban : Screen("kanban/{boardId}") {
        fun createRoute(boardId: String) = "kanban/$boardId"
    }
    data object CardDetail : Screen("card/{cardId}") {
        fun createRoute(cardId: Long) = "card/$cardId"
    }
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")
}

@Composable
fun FizzyNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.initializeAuth()
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Boards.route else Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Boards.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Boards.route) {
            BoardListScreen(
                onBoardClick = { boardId ->
                    navController.navigate(Screen.Kanban.createRoute(boardId))
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Kanban.route,
            arguments = listOf(navArgument("boardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: return@composable
            KanbanScreen(
                boardId = boardId,
                onBackClick = { navController.popBackStack() },
                onCardClick = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                }
            )
        }

        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNotificationClick = { notification ->
                    notification.cardId?.let { cardId ->
                        navController.navigate(Screen.CardDetail.createRoute(cardId))
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Boards.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
