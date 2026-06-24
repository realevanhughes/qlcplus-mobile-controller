package com.evanh.qlc_controller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualConsoleScreen(vm: ControlViewModel) {

    var pageIndex by remember { mutableIntStateOf(0) }

    var universe by remember { mutableIntStateOf(vm.defaultUniverse.intValue) }
    val pageSize = vm.pageSize.intValue

    val totalChannels = 512
    val totalPages = (totalChannels + pageSize - 1) / pageSize

    var channelValues by remember { mutableStateOf(IntArray(512)) }
    var channelMeta by remember { mutableStateOf(Array(512){ "" }) }

    val startCh = (pageIndex * pageSize + 1).coerceIn(1, 512)
    val endCh = (startCh + pageSize - 1).coerceAtMost(512)
    val visibleChannels = (startCh..endCh).toList()

    val scope = rememberCoroutineScope()

    var pageDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(universe, pageIndex, pageSize) {
        while (true) {
            vm.rUni(universe, pageIndex, pageSize)
            delay(vm.dmxRefresh.longValue.milliseconds)
        }
    }


    LaunchedEffect(vm.wsIncoming) {
        vm.wsIncoming.collect { msg ->
            if (msg == null) return@collect
            if (!msg.startsWith("QLC+API|getChannelsValues")) return@collect

            val parts = msg.split("|")
            val newArr = channelValues.clone()
            val newMetaArr = channelMeta.clone()

            var i = 2

            while (i < parts.size) {
                val ch = parts[i].toIntOrNull()
                val value = parts[i + 1].toIntOrNull()
                var meta = ""
                if ("#" in parts[i+2]) {
                    meta = parts[i+2].split(".")[1]
                }

                if (ch != null && value != null && ch in 1..512) {
                    newArr[ch - 1] = value
                    newMetaArr[ch - 1] = meta
                }

                i += when (vm.qlcVersion.intValue) {
                    4 -> {
                        3
                    }

                    5 -> {
                        4
                    }

                    else -> {
                        3
                    }
                }
            }

            channelValues = newArr
            channelMeta = newMetaArr
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = pageDropdownExpanded,
            onExpandedChange = { pageDropdownExpanded = !pageDropdownExpanded }
        ) {
            OutlinedTextField(
                value = "Page ${pageIndex + 1} / $totalPages",
                onValueChange = {},
                readOnly = true,
                label = { Text("Page") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = pageDropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = pageDropdownExpanded,
                onDismissRequest = { pageDropdownExpanded = false }
            ) {
                (0 until totalPages).forEach { index ->
                    DropdownMenuItem(
                        text = { Text("Page ${index + 1}") },
                        onClick = {
                            pageIndex = index
                            pageDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            visibleChannels.forEach { ch ->
                val v = channelValues[ch - 1]
                val m = channelMeta[ch - 1]
                VerticalFader(
                    channel = ch,
                    value = v,
                    onValueChange = { newVal ->
                        val updated = channelValues.clone()
                        updated[ch - 1] = newVal
                        channelValues = updated
                        scope.launch {
                            vm.cc(ch, newVal / 255f)
                        }
                    },
                    meta = m
                )
            }
        }
    }
}

@Composable
fun VerticalFader(channel: Int, value: Int, onValueChange: (Int) -> Unit, meta: String) {

    var sliderVal by remember { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        sliderVal = value
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {

        Text(
            "Ch $channel"
        )

        Slider(
            value = sliderVal / 255f,
            onValueChange = { new ->
                sliderVal = (new * 255).toInt()
                onValueChange(sliderVal)
            },
            modifier = Modifier
                .width(240.dp)
                .rotate(-90f)
                .padding(vertical = 30.dp),
            colors = SliderDefaults.colors(
                thumbColor = (if (meta != "" && meta != "#000000") Color(meta.toColorInt()) else MaterialTheme.colorScheme.secondary),
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        )

        Text(
            "$sliderVal"
        )
    }
}
