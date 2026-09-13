package com.vibecheck.lifepulse.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.ui.dashboard.DashboardScreen
import com.vibecheck.lifepulse.ui.expenses.ExpenseScreen
import com.vibecheck.lifepulse.ui.habits.HabitScreen
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography

sealed class Destination(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Dashboard : Destination("dashboard", R.string.nav_dashboard, Icons.Default.Home)
    data object Habits : Destination("habits", R.string.nav_habits, Icons.Default.CheckCircle)
    data object Expenses : Destination("expenses", R.string.nav_expenses, Icons.Default.Payments)

    companion object {
        val bottomBarItems = listOf(Dashboard, Habits, Expenses)
    }
}

/**
 * Navigates to a top-level tab destination, reusing the standard single-top pattern:
 * - Pops up to the graph's start destination (saving state of what's popped).
 * - Avoids launching multiple copies of the same destination on top of the stack.
 * - Restores previously saved state when re-selecting a tab.
 *
 * Using this helper everywhere (bottom bar AND in-screen "See all" links) keeps the
 * back stack consistent so tapping the bottom bar always works as expected.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun LifePulseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        containerColor = NeoColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = NeoColors.Surface,
                modifier = Modifier.border(
                    border = BorderStroke(3.dp, NeoColors.Border)
                )
            ) {
                Destination.bottomBarItems.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigateToTab(destination.route) },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = label,
                                tint = NeoColors.OnSurface
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = NeoTypography.labelSmall,
                                color = NeoColors.OnSurface
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeoColors.OnSurface,
                            unselectedIconColor = NeoColors.OnSurface.copy(alpha = 0.5f),
                            selectedTextColor = NeoColors.OnSurface,
                            unselectedTextColor = NeoColors.OnSurface.copy(alpha = 0.5f),
                            indicatorColor = NeoColors.Primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Dashboard.route) {
                DashboardScreen(
                    onSeeAllHabits = { navController.navigateToTab(Destination.Habits.route) },
                    onSeeAllExpenses = { navController.navigateToTab(Destination.Expenses.route) }
                )
            }
            composable(Destination.Habits.route) { HabitScreen() }
            composable(Destination.Expenses.route) { ExpenseScreen() }
        }
    }
}


