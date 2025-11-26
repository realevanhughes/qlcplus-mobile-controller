package com.evanh.qlc_controller

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.navigation.compose.*
import com.evanh.qlc_controller.ui.theme.QlccontrollerTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider


class MainActivity : ComponentActivity() {
    private lateinit var vm: ControlViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SettingsRepository(this)

        vm = ViewModelProvider(
            this,
            ControlViewModelFactory(repo)
        )[ControlViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            QlccontrollerTheme {
                val navController = rememberNavController()
                GlobalMessageHost(vm)

                Scaffold(
                    bottomBar = {
                        BottomNavBar(navController, vm)
                    }
                ) { padding ->

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Settings.route,
                        modifier = androidx.compose.ui.Modifier.padding(padding)
                    ) {

                        composable(Screen.Settings.route) {
                            SettingsScreen(vm) {
                                navController.navigate(Screen.Keypad.route)
                            }
                        }
                        composable(Screen.Keypad.route) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET && !vm.connected.value) {
                                WebSocketRequiredScreen(onRetry = { DmxKeypadScreen(vm) }, vm, navController)
                            } else {
                                LockOrientationScreen {
                                    DmxKeypadScreen(vm)
                                }
                            }
                        }
                        composable(Screen.Monitor.route) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET && !vm.connected.value) {
                                WebSocketRequiredScreen(onRetry = { DmxMonitorScreen(vm) }, vm, navController)
                            } else {
                                DmxMonitorScreen(vm)
                            }
                        }
                        composable(Screen.Console.route) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET && !vm.connected.value) {
                                WebSocketRequiredScreen(onRetry = { VirtualConsoleScreen(vm) }, vm, navController)
                            } else {
                                VirtualConsoleScreen(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun LockOrientationScreen(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember { context as Activity }

    // Remember original orientation
    val originalOrientation = remember { activity.requestedOrientation }

    DisposableEffect(Unit) {
        // Lock orientation to portrait while this screen is active
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            // Restore original orientation when leaving the screen
            activity.requestedOrientation = originalOrientation
        }
    }

    content()
}