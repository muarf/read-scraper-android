package com.readscraper.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Scraper : Screen("scraper", "Scraper", Icons.Default.Search)
    object Articles : Screen("articles", "Historique", Icons.Default.Article)
    object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Screen.Scraper,
        Screen.Articles,
        Screen.Settings
    )
    
    NavigationBar(modifier = modifier) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Éviter les multiples copies de la même destination dans la pile
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Réutiliser l'état sauvegardé si on revient sur la destination
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

