package com.prog7313.buglogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prog7313.buglogger.ui.screen.BugListScreen
import com.prog7313.buglogger.ui.screens.AddBugScreen
import com.prog7313.buglogger.viewmodel.AddBugViewModel
import com.prog7313.buglogger.ui.screens.BugDetailScreen
import com.prog7313.buglogger.viewmodel.BugDetailViewModel
import com.prog7313.buglogger.viewmodel.BugListViewModel

object Routes {
    const val BUG_LIST = "bug_list"
    const val ADD_BUG = "add_bug"
    const val BUG_DETAIL = "bug_detail"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.BUG_LIST, modifier = modifier) {

        composable(Routes.BUG_LIST) {
            val viewModel: BugListViewModel = viewModel()
            BugListScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate(Routes.ADD_BUG) },
                onBugClick = { id -> navController.navigate("${Routes.BUG_DETAIL}/$id") }
            )
        }

        composable(Routes.ADD_BUG) {
            val viewModel: AddBugViewModel = viewModel()
            AddBugScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "${Routes.BUG_DETAIL}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val viewModel: BugDetailViewModel = viewModel()
            BugDetailScreen(
                bugId = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

    }
}