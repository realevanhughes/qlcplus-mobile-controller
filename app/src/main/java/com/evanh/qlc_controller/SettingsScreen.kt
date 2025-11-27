package com.evanh.qlc_controller

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

fun getAppVersionInfo(context: Context): Pair<String, Long> {
    val pm = context.packageManager
    val pi = pm.getPackageInfo(context.packageName, 0)
    val versionName = pi.versionName ?: "N/A"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong()
    return versionName to versionCode
}

@Composable
fun RowScope.ModeButton(
    label: String,
    mode: ControlMode,
    current: ControlMode,
    onChange: (ControlMode) -> Unit
) {
    val selected = (mode == current)

    Button(
        onClick = { onChange(mode) },
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun SettingsScreen(
    vm: ControlViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var refreshSec by remember { mutableLongStateOf(vm.DMXRefresh.longValue) }
    var fade by remember { mutableLongStateOf(vm.DMXFade.longValue) }

    var useIconLabels by remember { mutableStateOf(vm.useIconLabels.value) }
    var useHaptics by remember { mutableStateOf(vm.useHaptics.value) }
    var useSettingsPopups by remember { mutableStateOf(vm.useSettingsPopups.value) }

    var ipText by remember { mutableStateOf(vm.ip.value) }
    var portText by remember { mutableStateOf(vm.port.value.toString()) }

    var universeCountText by remember { mutableStateOf(vm.universeCount.value.toString()) }
    var defaultUniverseText by remember { mutableStateOf(vm.defaultUniverse.value.toString()) }
    var pageSizeText by remember { mutableStateOf(vm.pageSize.value.toString()) }

    val ipValid = ipText.isNotBlank()
    val portValid = portText.toIntOrNull() != null
    val uniCountValid = universeCountText.toIntOrNull()?.let { it in 1..99 } == true
    val uniValid = defaultUniverseText.toIntOrNull()?.let { it in 1..99 } == true
    val pageValid = pageSizeText.toIntOrNull()?.let { it in listOf(12, 24, 48, 96) } == true

    val allValid = ipValid && portValid && uniValid && uniCountValid && pageValid

    val (versionName, versionCode) = getAppVersionInfo(LocalContext.current)

    LaunchedEffect(vm) {
        if (vm.connected.value) {
            vm.disconnectWebSocket()
        }
    }

    fun saveSettings() {
        vm.updateIp(ipText)
        vm.updatePort(portText.toInt())
        vm.updateUniverseCount(universeCountText.toInt())
        vm.updateDefaultUniverse(defaultUniverseText.toInt())
        vm.updatePageSize(pageSizeText.toInt())
        vm.updateDMXRefresh(refreshSec)
        vm.updateDMXFade(fade)
        vm.updateIconLabels(useIconLabels)
        vm.updateHaptics(useHaptics)
        vm.updateSettingsPopups(useSettingsPopups)
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    enabled = allValid,
                    onClick = {
                        saveSettings()
                        if (allValid) {
                            if (vm.controlMode.value == ControlMode.WEBSOCKET) vm.connectWebSocket()
                            onDone()
                        }
                        else {
                            vm.showMessage("Error","Invalid settings.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Continue")
                }
            }
        }
    ) { padding ->
        Text("Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
        )
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Control Mode", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))

            val controlOptions = listOf(ControlMode.NONE, ControlMode.WEBSOCKET)
            val selectedControlIndex = remember { mutableStateOf(controlOptions.indexOf(vm.controlMode.value)) }

            MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                controlOptions.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = controlOptions.size
                        ),
                        checked = selectedControlIndex.value == index,
                        onCheckedChange = {
                            selectedControlIndex.value = index
                            vm.controlMode.value = mode
                            saveSettings()
                        },
                        icon = {
                            when (mode) {
                                ControlMode.NONE -> Icon(Icons.Default.Clear, contentDescription = "None")
                                ControlMode.WEBSOCKET -> Icon(Icons.Default.Power, contentDescription = "WebSocket")
                            }
                        },
                        label = {
                            Text(
                                when (mode) {
                                    ControlMode.NONE -> "None"
                                    ControlMode.WEBSOCKET -> "WebSocket"
                                }
                            )
                        },
                    )
                }
            }


            if (vm.controlMode.value == ControlMode.NONE && vm.useSettingsPopups.value) {
                (Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("Running in dummy mode — no control connection set.",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("QLC Host Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                label = { Text("IP Address") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { vm.showMessage("Info","This is the IP address of your QLC host computer.\nThis is usually something like:\n192.168.1.1") }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Help"
                        )
                    }
                }
            )

            if (try { java.net.InetAddress.getByName(ipText); false } catch (e: Exception) { true } && vm.useSettingsPopups.value) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Icon(Icons.Default.Warning, null, modifier=Modifier.padding(12.dp))
                    Text("This IP address is invalid and will not connect to QLC+.",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = portText,
                onValueChange = {
                    portText = it
                    saveSettings()
                },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { vm.showMessage("Info","This is the port which is used to connect to the host.\nThis must be forwarded in the host's firewall.\nThe default port on QLC+ is 9999.") }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Help"
                        )
                    }
                }
            )

            if (portText != "9999" && vm.useSettingsPopups.value) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("The default port for QLC+ web interface (and websockets) is 9999.",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Interface", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = universeCountText,
                onValueChange = {
                    universeCountText = it
                    saveSettings()
                },
                label = { Text("Total Universes") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { vm.showMessage("Info","How many total DMX universes you can choose from.\nNot used very much in this application.") }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Help"
                        )
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = defaultUniverseText,
                onValueChange = {
                    defaultUniverseText = it
                    saveSettings()
                },
                label = { Text("Default Universe") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { vm.showMessage("Info","The default port QLC+ controller will change/read when an interaction is made.\nUsually this is left at 1.") }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Help"
                        )
                    }
                }
            )

            if (defaultUniverseText != "1" && vm.useSettingsPopups.value) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("Due to QLC+ websocket support, controlling multiple universes is not in a working state.",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Channels per page:", style = MaterialTheme.typography.bodyMedium)
            DropdownMenuSwitcher(
                current = pageSizeText,
                options = listOf("12", "24", "48", "96")
            ) { selected ->
                pageSizeText = selected
                saveSettings()
            }

            if (pageSizeText.toInt() > 24 && vm.useSettingsPopups.value) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("Controlling or polling large numbers of fixtures requires lots of bandwidth.",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show keypad icons instead of words")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = useIconLabels,
                    onCheckedChange = {
                        useIconLabels = it
                        saveSettings()
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use haptic feedback")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = useHaptics,
                    onCheckedChange = {
                        useHaptics = it
                        saveSettings()
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show helpful advice")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = useSettingsPopups,
                    onCheckedChange = {
                        useSettingsPopups = it
                        saveSettings()
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Refresh rate: ${refreshSec}ms")

            Slider(
                value = refreshSec.toFloat(),
                onValueChange = { newVal ->
                    refreshSec = newVal.toLong()
                    saveSettings()
                },
                valueRange = 20f..2000f,
                steps = 10
            )

            if (refreshSec > 400f && vm.useSettingsPopups.value) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("Slow refresh rate saves on bandwidth but makes the app less responsive.",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Text("Sequential fade: ${fade}ms")

            Slider(
                value = fade.toFloat(),
                onValueChange = { newVal ->
                    fade = newVal.toLong()
                    saveSettings()
                },
                valueRange = 0f..1000f,
                steps = 10
            )

            if (fade > 400f && vm.useSettingsPopups.value) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier=Modifier.padding(12.dp))
                    Text("Large fade times consumes lots of memory and CPU. Looks cool but at what cost?",
                        modifier=Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("About", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(12.dp))
            val context = LocalContext.current
            val isDark = isSystemInDarkTheme()

            val githubIcon = if (isDark) {
                R.drawable.github_mark_white
            } else {
                R.drawable.github_mark
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFBE9236)),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "You are running version $versionCode of QLC+ mobile controller built by Evan Hughes.",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    AssistChip(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/realevanhughes/qlcplus-mobile-controller".toUri()
                            )
                            context.startActivity(intent)
                        },
                        label = { Text("View on GitHub") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = githubIcon),
                                contentDescription = "GitHub",
                                tint = Color.Unspecified,
                                modifier = Modifier.height(20.dp).width(20.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
