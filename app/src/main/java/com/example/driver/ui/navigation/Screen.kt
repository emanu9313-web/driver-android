package com.example.driver.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object PassengerHome : Screen("passenger_home")
    object DriverHome : Screen("driver_home")
    object AdminHome : Screen("admin_home")
    object History : Screen("history")
    object Profile : Screen("profile")
}
