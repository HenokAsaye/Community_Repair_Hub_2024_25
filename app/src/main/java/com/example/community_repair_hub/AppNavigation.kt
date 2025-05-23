package com.example.community_repair_hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.community_repair_hub.Screen.*

import com.example.community_repair_hub.Utills.TokenManager
import com.example.community_repair_hub.ViewModel.*

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val profileImageUrl by homeViewModel.profileImageUrl.collectAsState()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(modifier, navController)
        }
        composable("login") {
            LoginScreen(modifier, navController)
        }
        composable("signup") {
            SignupScreen(modifier, navController)
        }
        composable("home") {
            HomeScreen(modifier, navController, viewModel = homeViewModel)
        }
        composable("report") {
            ReportIssueScreen(modifier, navController)
        }
        composable("viewdetail/{issueId}") { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            ViewDetailScreen(issueId = issueId, navController = navController)
        }
        composable("repairhome") {
            RepairTeamHomeScreen(
                modifier = modifier,
                navController = navController,
                viewModel = RepairTeamHomeViewModel()
            )
        }
        composable("repairDetail") {
            RepairDetailScreen(modifier, navController)
        }
        composable("repairviewdetail/{issueId}") { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            RepairViewDetailScreen(issueId = issueId, navController = navController)
        }
        composable("assignedissue/{issueId}") { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            AssignedIssueScreen(issueId = issueId, navController = navController)
        }
    }
}