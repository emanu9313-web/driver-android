package com.example.driver.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.driver.domain.model.UserRole
import com.example.driver.ui.screens.DriverControlScreen
import com.example.driver.ui.screens.HistoryScreen
import com.example.driver.ui.screens.LoginScreen
import com.example.driver.ui.screens.PassengerScreen
import com.example.driver.ui.screens.ProfileScreen
import com.example.driver.ui.screens.RegisterScreen
import com.example.driver.ui.viewmodel.AdminViewModel
import com.example.driver.ui.viewmodel.AuthViewModel
import com.example.driver.ui.viewmodel.DriverViewModel
import com.example.driver.ui.viewmodel.PassengerViewModel

@Composable
fun TaxiNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination = if (currentUser == null) {
        Screen.Login.route
    } else {
        when (currentUser!!.role) {
            UserRole.PASAJERO -> Screen.PassengerHome.route
            UserRole.CONDUCTOR -> Screen.DriverHome.route
            UserRole.ADMIN -> Screen.AdminHome.route
        }
    }

    val showBottomBar = currentUser != null && currentRoute in listOf(
        Screen.PassengerHome.route,
        Screen.DriverHome.route,
        Screen.AdminHome.route,
        Screen.History.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val homeRoute = when (currentUser?.role) {
                        UserRole.CONDUCTOR -> Screen.DriverHome.route
                        UserRole.ADMIN -> Screen.AdminHome.route
                        else -> Screen.PassengerHome.route
                    }

                    NavigationBarItem(
                        selected = currentRoute == homeRoute,
                        onClick = { navController.navigate(homeRoute) { popUpTo(homeRoute) { inclusive = true } } },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio", fontWeight = if (currentRoute == homeRoute) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFFEF08A),
                            selectedIconColor = Color(0xFFD97706),
                            selectedTextColor = Color(0xFFD97706)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.History.route,
                        onClick = { navController.navigate(Screen.History.route) },
                        icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                        label = { Text("Historial", fontWeight = if (currentRoute == Screen.History.route) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFFEF08A),
                            selectedIconColor = Color(0xFFD97706),
                            selectedTextColor = Color(0xFFD97706)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = { navController.navigate(Screen.Profile.route) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontWeight = if (currentRoute == Screen.Profile.route) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFFEF08A),
                            selectedIconColor = Color(0xFFD97706),
                            selectedTextColor = Color(0xFFD97706)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.PassengerHome.route) {
                val passengerViewModel: PassengerViewModel = hiltViewModel()
                PassengerScreen(viewModel = passengerViewModel)
            }

            composable(Screen.DriverHome.route) {
                val driverViewModel: DriverViewModel = hiltViewModel()
                DriverControlScreen(viewModel = driverViewModel)
            }

            composable(Screen.AdminHome.route) {
                val adminViewModel: AdminViewModel = hiltViewModel()
                DriverControlScreen(viewModel = hiltViewModel())
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = hiltViewModel())
            }

            composable(Screen.Profile.route) {
                ProfileScreen(authViewModel = authViewModel)
            }
        }
    }
}
