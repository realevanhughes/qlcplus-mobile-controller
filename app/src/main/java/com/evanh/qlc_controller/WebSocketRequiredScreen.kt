package com.evanh.qlc_controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun WebSocketRequiredScreen(onRetry: @Composable () -> Unit, vm: ControlViewModel, navController: NavController) {
    var waiting by remember { mutableStateOf(true) }
    var autoReEntry by remember { mutableStateOf(false) }
    var largeIco by remember { mutableStateOf(Icons.Default.Close) }
    var largeIcoColor by remember { mutableStateOf(Color(0xFFA41E1E)) }
    if (waiting) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Waiting for connection...")
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
    else {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.size(200.dp))
            Icon(
                imageVector = largeIco,
                tint = largeIcoColor,

                modifier = Modifier.size(96.dp),
                contentDescription = "Error"
            )
            Spacer(Modifier.height(20.dp))
            Text("WebSocket is not connected")
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = !autoReEntry,
                    onClick = {
                    if (!vm.connected.value) {
                        vm.connectWebSocket()
                    } else {
                        onRetry
                    }
                    waiting = true
                }) {
                    Text("Retry Connection")
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = if (autoReEntry) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Auto re-entry")
                        Spacer(Modifier.width(15.dp))
                        Switch(
                            checked = autoReEntry,
                            onCheckedChange = { autoReEntry = it }
                        )
                    }
                }
            }
            if (vm.useSettingsPopups.value) {
                Spacer(modifier = Modifier.weight(1f))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("Hint:\nIf you are just testing the app's interface, use connection mode 'None' for a dummy connection!",
                        modifier=Modifier.padding(12.dp))
                    Button(
                        onClick = {
                            navController.navigate("settings")
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Go")
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
        }
        }
    }
    LaunchedEffect(waiting) {
        delay(2000L)
        waiting = false
    }

    LaunchedEffect(autoReEntry, largeIco, largeIcoColor) {
        while (true) {
            delay(1000L)
            if (autoReEntry) {
                vm.connectWebSocket()
                largeIco = Icons.Default.Schedule
                largeIcoColor = Color(0xFFFF9800)
                delay(4000L)
            }
            largeIco = Icons.Default.Close
            largeIcoColor = Color(0xFFA41E1E)
        }

    }
}
