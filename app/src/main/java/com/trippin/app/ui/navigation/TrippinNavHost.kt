package com.trippin.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trippin.app.di.AppContainer
import com.trippin.app.ui.screens.cars.CarsScreen
import com.trippin.app.ui.screens.home.HomeScreen
import com.trippin.app.ui.screens.refuel.RefuelScreen
import com.trippin.app.ui.screens.settings.SettingsScreen
import com.trippin.app.ui.screens.trips.TripDetailScreen
import com.trippin.app.ui.screens.trips.TripsScreen

sealed class BottomNavItem(val route: String, val label: String) {
    data object Home : BottomNavItem("home", "Home")
    data object Trips : BottomNavItem("trips", "Trips")
    data object Refuel : BottomNavItem("refuel", "Refuel")
    data object Cars : BottomNavItem("cars", "Cars")
    data object Settings : BottomNavItem("settings", "Settings")
}

@Composable
fun TrippinNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Trips,
        BottomNavItem.Refuel,
        BottomNavItem.Cars,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute?.startsWith("trip/") != true) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        BottomNavItem.Home -> Icons.Default.Home
                                        BottomNavItem.Trips -> Icons.Default.Route
                                        BottomNavItem.Refuel -> Icons.Default.LocalGasStation
                                        BottomNavItem.Cars -> Icons.Default.DirectionsCar
                                        BottomNavItem.Settings -> Icons.Default.Settings
                                    },
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    container = container,
                    onTripClick = { navController.navigate("trip/$it") }
                )
            }
            composable(BottomNavItem.Trips.route) {
                TripsScreen(
                    container = container,
                    onTripClick = { navController.navigate("trip/$it") }
                )
            }
            composable("trip/{tripId}") { entry ->
                val tripId = entry.arguments?.getString("tripId") ?: return@composable
                TripDetailScreen(
                    tripId = tripId,
                    container = container,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Refuel.route) {
                RefuelScreen(container = container)
            }
            composable(BottomNavItem.Cars.route) {
                CarsScreen(container = container)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(container = container)
            }
        }
    }
}
