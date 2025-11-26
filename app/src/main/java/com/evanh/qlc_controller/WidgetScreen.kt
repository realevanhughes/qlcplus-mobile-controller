package com.evanh.qlc_controller

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.evanh.qlc_controller.VcWidget

sealed class VcWidget(
    open val id: Int,
    open val name: String
) {
    data class Button(
        override val id: Int,
        override val name: String,
        val isOn: Boolean = false
    ) : VcWidget(id, name)

    data class Slider(
        override val id: Int,
        override val name: String,
        val value: Int = 0 // Represents 0-255
    ) : VcWidget(id, name)

    data class Frame(
        override val id: Int,
        override val name: String
    ) : VcWidget(id, name)

    data class Other(
        override val id: Int,
        override val name: String,
        val widgetType: String
    ) : VcWidget(id, name)
}


// --- MAIN SCREEN COMPOSABLE ---
@Composable
fun WidgetScreen(vm: ControlViewModel) {

    // This LaunchedEffect listens for messages and calls the parsing logic
    LaunchedEffect(Unit) {
        vm.wsIncoming.collect { msg ->
            if (msg == null) return@collect
            // Call the parser function defined below
            parseWebSocketMessage(msg, vm)
        }
    }

    // This effect triggers the initial data fetch
    LaunchedEffect(vm.connected.value) {
        if (vm.connected.value && vm.controlMode.value == ControlMode.WEBSOCKET) {
            // This VM function just sends the "getWidgetsList" message
            vm.fetchVCWidgets()
        }
    }

    // Observe the widget list from the ViewModel
    val widgets = vm.vcWidgets.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            "Virtual Console Widgets",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(widgets) { widget ->
                // The 'when' statement renders the correct composable for each widget type
                when (widget) {
                    is VcWidget.Button -> VCButtonCell(vm, widget)
                    is VcWidget.Slider -> VCSliderCell(vm, widget)
                    is VcWidget.Frame -> VCFrameCell(widget)
                    is VcWidget.Other -> VCOtherCell(widget)
                }
            }
        }
    }
}

// --- PARSING LOGIC ---
// These functions live in WidgetScreen.kt and are called by the LaunchedEffect

/**
 * Main parser function to handle all incoming websocket messages.
 * This function directly modifies the ViewModel's state.
 */
fun parseWebSocketMessage(msg: String, vm: ControlViewModel) {
    Log.d("DEBUG", "VM processing: $msg")
    val parts = msg.split("|")

    // --- Handle API responses ---
    if (parts.getOrNull(0) == "QLC+API") {
        when (parts.getOrNull(1)) {
            // Step 1: We get the list of all widgets
            "getWidgetsList" -> {
                vm.pendingWidgets.clear()
                vm.vcWidgets.value = emptyList() // Clear the UI

                // Parse the ID/Name pairs into our temporary map
                val widgetsMap = parseWidgetList(msg)
                vm.pendingWidgets.putAll(widgetsMap)

                // Step 2: Request the type for each widget
                widgetsMap.keys.forEach { widgetId ->
                    vm.wsClient.send("QLC+API|getWidgetType|$widgetId")
                }
            }

            // Step 3: We get the type for a specific widget
            "getWidgetType" -> {
                val id = parts.getOrNull(2)?.toIntOrNull()
                val type = parts.getOrNull(3)

                if (id != null && type != null && vm.pendingWidgets.containsKey(id)) {
                    val name = vm.pendingWidgets.remove(id)!!

                    val newWidget = when (type) {
                        "Button" -> VcWidget.Button(id, name)
                        "Slider" -> VcWidget.Slider(id, name)
                        "Frame" -> VcWidget.Frame(id, name)
                        "AudioTriggers" -> VcWidget.Other(id, name, type)
                        "XYPad" -> VcWidget.Other(id, name, type)
                        else -> VcWidget.Other(id, name, type)
                    }

                    // Add the new, fully-typed widget to the UI list
                    vm.vcWidgets.value += newWidget
                }
            }
        }
        return // It was an API message, we're done.
    }

    // --- Handle FUNCTION state updates ---
    if (parts.getOrNull(0) == "FUNCTION") {
        val funcId = parts.getOrNull(1)?.toIntOrNull() ?: return
        val isOn = parts.getOrNull(2) == "Running"

        vm.vcWidgets.value = vm.vcWidgets.value.map {
            // WARNING: This assumes Widget ID == Function ID.
            if (it.id == funcId && it is VcWidget.Button) {
                it.copy(isOn = isOn)
            } else {
                it
            }
        }
        return
    }

    // --- Handle SLIDER state updates ---
    // A simple "ID|VALUE" message
    if (parts.size == 2 && parts.getOrNull(0)?.toIntOrNull() != null) {
        val id = parts[0].toInt()
        val value = parts[1].toIntOrNull() ?: return

        vm.vcWidgets.value = vm.vcWidgets.value.map {
            if (it.id == id && it is VcWidget.Slider) {
                it.copy(value = value)
            } else {
                it
            }
        }
        return
    }
}

/**
 * Helper function to parse the initial "getWidgetsList" string.
 */
private fun parseWidgetList(msg: String): Map<Int, String> {
    val parts = msg.split("|")
    val out = mutableMapOf<Int, String>()

    var i = 2 // Start after "QLC+API|getWidgetsList"
    while (i + 1 < parts.size) {
        val id = parts[i].toIntOrNull()
        val name = parts[i + 1].trim()

        if (id != null && name.isNotEmpty()) {
            out[id] = name
        }

        i += 2
    }
    return out
}


// --- WIDGET COMPOSABLES ---

@Composable
fun VCButtonCell(vm: ControlViewModel, btn: VcWidget.Button) {
    val bg = if (btn.isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (btn.isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(btn.id) {
                detectTapGestures(
                    onPress = {
                        vm.wsClient.send("${btn.id}|1") // PRESS = 1
                        try {
                            awaitRelease()
                        } finally {
                            vm.wsClient.send("${btn.id}|0") // RELEASE = 0
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(btn.name, color = textColor)
        }
    }
}

@Composable
fun VCSliderCell(vm: ControlViewModel, slider: VcWidget.Slider) {
    var sliderPosition by remember(slider.value) { mutableStateOf(slider.value / 255f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(slider.name)
            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    sliderPosition = newValue
                },
                onValueChangeFinished = {
                    val intValue = (sliderPosition * 255).toInt()
                    vm.wsClient.send("${slider.id}|$intValue")
                }
            )
        }
    }
}

@Composable
fun VCFrameCell(frame: VcWidget.Frame) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(frame.name, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun VCOtherCell(widget: VcWidget.Other) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("${widget.name}\n(${widget.widgetType})", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}