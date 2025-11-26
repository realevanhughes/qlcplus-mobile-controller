package com.evanh.qlc_controller

sealed class Screen(val route: String, val title: String) {
    object Settings : Screen("settings", "Settings")
    object Keypad : Screen("keypad", "Keypad")

    object Monitor: Screen("monitor", "Monitor")
    object Console: Screen("console", "Console")
}
