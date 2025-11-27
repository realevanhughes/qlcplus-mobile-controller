package com.evanh.qlc_controller

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Tune

@Composable
fun BottomNavBar(navController: NavController, vm: ControlViewModel) {
    val items = listOf(
        Screen.Settings,
        Screen.Keypad,
        Screen.Monitor,
        Screen.Console,
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                enabled =
                    when (screen) {
                        Screen.Settings -> true
                        Screen.Keypad -> vm.controlMode.value != ControlMode.WEBSOCKET || vm.connected.value
                        Screen.Monitor -> vm.controlMode.value != ControlMode.WEBSOCKET || vm.connected.value
                        Screen.Console -> vm.controlMode.value != ControlMode.WEBSOCKET || vm.connected.value
                    },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Settings.route)
                        launchSingleTop = true
                    }
                },
                label = { Text(screen.title) },
                icon = {
                    when (screen) {
                        Screen.Settings ->
                            Icon(Icons.Default.Settings, contentDescription = "Settings")

                        Screen.Keypad ->
                            Icon(Icons.Default.Dialpad, contentDescription = "Keypad")

                        Screen.Monitor ->
                            Icon(Icons.Default.Monitor, contentDescription = "Monitor")

                        Screen.Console ->
                            Icon(Icons.Default.Tune, contentDescription = "Console")

                    }
                }

            )
        }
    }
}
