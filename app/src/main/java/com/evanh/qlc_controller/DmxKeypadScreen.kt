package com.evanh.qlc_controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val success: Boolean
)

@Composable
fun KeyButton(
    text: String,
    useIconLabels: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor
        )
    ) {
        var changed = false
        if (useIconLabels && text == "BACK" && !changed) {
            Icon(Icons.Default.Backspace, contentDescription = "Backspace")
            changed = true
        }
        if (useIconLabels && text == "ENTER" && !changed) {
            Icon(Icons.Default.Send, contentDescription = "Enter", tint = MaterialTheme.colorScheme.onSurface)
            changed = true
        }
        if (useIconLabels && text == "THRU" && !changed) {
            Icon(Icons.Default.MultipleStop, contentDescription = "THRU")
            changed = true
        }
        if (useIconLabels && text == "AT" && !changed) {
            Icon(Icons.Default.Edit, contentDescription = "AT")
            changed = true
        }
        if (useIconLabels && text == "FULL" && !changed) {
            Icon(Icons.Default.BrightnessHigh, contentDescription = "FULL")
            changed = true
        }
        if (useIconLabels && text == "ZERO" && !changed) {
            Icon(Icons.Default.BrightnessLow, contentDescription = "ZERO")
            changed = true
        }
        if (useIconLabels && text == "UNI" && !changed) {
            Icon(Icons.Default.Public, contentDescription = "UNI")
            changed = true
        }
        if (useIconLabels && text == "CLR" && !changed) {
            Icon(Icons.Default.Clear, contentDescription = "CLR")
            changed = true
        }
        if (!changed) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun DmxKeypadScreen(vm: ControlViewModel) {

    val scope = rememberCoroutineScope()
    var command by remember { mutableStateOf("") }

    var commandHistory = remember { mutableStateListOf<CommandEntry>() }
    val listState = rememberLazyListState()

    LaunchedEffect(commandHistory.size) {
        if (commandHistory.isNotEmpty()) {
            scope.launch {
                listState.scrollToItem(0)
            }
        }
    }

    fun addCommand(command: String, success: Boolean) {
        val time = System.currentTimeMillis()
        commandHistory.add(
            0,
            CommandEntry(
                timestamp = time,
                command = command,
                success = success
            )
        )
    }
    fun clearHistory() {
        commandHistory.removeRange(0, commandHistory.size)
    }

    fun editCurrentCommand(newCommand: String) {
        command = newCommand
    }

    fun append(text: String) { command += text }
    fun clear() { command = "" }

    fun backspaceWord() {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            command = ""
            return
        }

        val parts = trimmed.split(" ")
        command = if (parts.size <= 1) "" else parts.dropLast(1).joinToString(" ")
    }

    fun sendEnter() {
        var exec = false
        val parts = command.trim().split(" ")

        if (command == "") {
            vm.showMessage("Error","Please enter a command.")
        }
        else {
            if (parts.size >= 3 && parts[1] == "AT") {
                val channel = parts[0].toIntOrNull()
                val value = parts[2].toIntOrNull()

                if (channel != null && value != null) {
                    scope.launch(Dispatchers.IO) {
                        vm.CC(channel, value / 255f)
                    }
                }
                exec = true
            }

            if (parts.size >= 3 && parts[1] == "+") {
                val channel = parts[0].toIntOrNull()
                val value = parts[2].toIntOrNull()
                if (channel != null && value != null) {
                    scope.launch(Dispatchers.IO) {
                        vm.RC(channel, vm.defaultUniverse.value)
                        vm.wsIncoming.collect { msg ->
                            if (msg == null) return@collect
                            if (!msg.startsWith("QLC+API|getChannelsValues")) return@collect
                            val parts = msg.split("|")
                            if ((parts[3].toInt().plus(value)).div(255f) !in 0f..1f) {
                                vm.showMessage("Info","The result of this operation is outside the allowed range 0 to 255.\n" +
                                        "It has been truncated to prevent fatal failure.\n" +
                                        "Next time please don't specify such a value.")
                            }
                            else {
                                vm.CC(channel, (parts[3].toInt().plus(value)).div(255f))
                            }
                        }
                    }
                }
                exec = true
            }

            if (parts.size >= 3 && parts[1] == "-") {
                val channel = parts[0].toIntOrNull()
                val value = parts[2].toIntOrNull()
                if (channel != null && value != null) {
                    scope.launch(Dispatchers.IO) {
                        vm.RC(channel, vm.defaultUniverse.value)
                        vm.wsIncoming.collect { msg ->
                            if (msg == null) return@collect
                            if (!msg.startsWith("QLC+API|getChannelsValues")) return@collect
                            val parts = msg.split("|")
                            if ((parts[3].toInt().minus(value)).div(255f) !in 0f..1f) {
                                vm.showMessage("Info","The result of this operation is outside the allowed range 0 to 255.\n" +
                                        "It has been truncated to prevent fatal failure.\n" +
                                        "Next time please don't specify such a value.")
                            }
                            else {
                                vm.CC(channel, (parts[3].toInt().minus(value)).div(255f))
                            }
                        }
                    }
                }
                exec = true
            }

            if (parts.size >= 2 && parts[1] == "CLR") {
                val channel = parts[0].toIntOrNull()

                if (channel != null) {
                    scope.launch(Dispatchers.IO) {
                        vm.CCReset(channel)
                    }
                }
                exec = true
            }
            if (parts.size >= 3 && parts[0] == "UNI" && parts[2] == "CLR") {
                val universe = parts[1].toIntOrNull()

                if (universe != null) {
                    scope.launch(Dispatchers.IO) {
                        vm.UniReset(universe)
                    }
                }
                exec = true
            }
            if (parts.size >= 3 && parts[0] == "UNI" && parts[2] == "AT") {
                val value = parts[3].toIntOrNull()?.coerceIn(0, 255) ?: return
                scope.launch {
                    vm.applyToRange(
                        universe = parts[1].toInt(),
                        channels = 1..512,
                        value = value
                    )
                }
                exec = true
            }
            if (parts.size >= 5 && parts[1] == "THRU" && parts[3] == "AT") {
                val value = parts[4].toIntOrNull()?.coerceIn(0, 255) ?: return
                scope.launch {
                    vm.applyToRange(
                        universe = vm.defaultUniverse.value,
                        channels = parts[0].toInt()..parts[2].toInt(),
                        value = value
                    )
                }
                exec = true
            }
            if (parts.size >= 4 && parts[1] == "THRU" && parts[3] == "CLR") {
                scope.launch {
                    vm.RangeReset(parts[0].toInt(), parts[2].toInt(), vm.defaultUniverse.value)
                }
                exec = true
            }

            addCommand(command, exec)
            if (!exec) {
                vm.showMessage("Error","Invalid command.")
            }
        }
        clear()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Command") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (command.isNotEmpty()) {
                    IconButton(onClick = { command = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear text"
                        )
                    }
                }
            }
        )

        Spacer(Modifier.height(20.dp))

        val keys = listOf(
            listOf("7", "8", "9", "AT"),
            listOf("4", "5", "6", "THRU"),
            listOf("1", "2", "3", "FULL"),
            listOf("-", "0", "+", "ZERO"),
            listOf("BACK", "UNI", "CLR", "ENTER")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        KeyButton(
                            text = key,
                            useIconLabels = vm.useIconLabels.value,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            color = when (key) {
                                "ENTER" -> Color(0xD6327635)
                                "CLR", "BACK" -> MaterialTheme.colorScheme.errorContainer
                                "AT", "THRU", "FULL", "ZERO", "UNI" ->
                                    MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = when (key) {
                                "ENTER" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "CLR", "BACK" -> MaterialTheme.colorScheme.onErrorContainer
                                "AT", "THRU", "FULL", "ZERO", "UNI" ->
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        ) {
                            when (key) {
                                "ENTER" -> sendEnter()
                                "AT" -> append(" AT ")
                                "THRU" -> append(" THRU ")
                                "FULL" -> append("255")
                                "ZERO" -> append("0")
                                "-" -> append(" - ")
                                "+" -> append(" + ")
                                "UNI" -> append("UNI ")
                                "BACK" -> backspaceWord()
                                "CLR" -> append(" CLR")
                                else -> append(key)
                            }
                        }
                    }
                }
            }
        }


        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CommandHistory(
                cmds = commandHistory,
                onClear = { clearHistory() },
                cmd = command,
                onCmdClick = { editCurrentCommand(it) },
                listState = listState,
            )
        }
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CommandHistory(
    cmds: List<CommandEntry>,
    onClear: () -> Unit = {},
    cmd: String,
    onCmdClick: (String) -> Unit,
    listState: LazyListState
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "History",
            style = MaterialTheme.typography.titleMedium
        )

        TextButton(onClick = onClear) {
            Text("Clear")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        state = listState
    ) {
        items(cmds, key = { it.timestamp }) { entry ->

            val time = remember(entry.timestamp) {
                dateFormat.format(Date(entry.timestamp))
            }

            AnimatedVisibility(
                visible = true,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (entry.success) Color(0xD6327635) else MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { onCmdClick(entry.command) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = entry.command,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Icon(
                            imageVector = if (entry.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (entry.success) "Success" else "Failed",
                            tint = if (entry.success)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(28.dp)
                        )
                    }
                }
            }
        }
    }
}


