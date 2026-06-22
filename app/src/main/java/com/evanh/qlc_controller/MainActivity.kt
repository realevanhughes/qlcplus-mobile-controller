package com.evanh.qlc_controller

import android.app.Activity
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
                            LockOrientationScreen({ SettingsScreen(vm) { navController.navigate(Screen.Keypad.route) } }, vm.lockOrientation.value)
                        }
                        composable(Screen.Keypad.route) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET && !vm.connected.value) {
                                WebSocketRequiredScreen(onRetry = { LockOrientationScreen({ DmxKeypadScreen(vm) }, true) }, vm, navController)
                            } else {
                                LockOrientationScreen({ DmxKeypadScreen(vm) }, true)
                            }
                        }
                        composable(Screen.Monitor.route) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET && !vm.connected.value) {
                                WebSocketRequiredScreen(onRetry = { LockOrientationScreen({ DmxMonitorScreen(vm) }, vm.lockOrientation.value) }, vm, navController)
                            } else {
                                LockOrientationScreen({ DmxMonitorScreen(vm) }, vm.lockOrientation.value)
                            }
                        }
                        composable(Screen.Console.route) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET && !vm.connected.value) {
                                WebSocketRequiredScreen(onRetry = { LockOrientationScreen({ VirtualConsoleScreen(vm) }, vm.lockOrientation.value) }, vm, navController)
                            } else {
                                LockOrientationScreen({ VirtualConsoleScreen(vm) }, vm.lockOrientation.value)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun LockOrientationScreen(
    content: @Composable () -> Unit,
    lockOrientation: Boolean,
) {
    val context = LocalContext.current
    val activity = remember { context as Activity }

    val originalOrientation = remember { activity.requestedOrientation }

    if (lockOrientation) {
        DisposableEffect(Unit) {

            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            onDispose {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    content()
}