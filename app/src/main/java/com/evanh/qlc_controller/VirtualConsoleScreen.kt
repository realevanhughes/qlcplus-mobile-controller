package com.evanh.qlc_controller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VirtualConsoleScreen(vm: ControlViewModel) {

    var pageIndex by remember { mutableStateOf(0) }

    var universe by remember { mutableStateOf(vm.defaultUniverse.value) }
    val pageSize = vm.pageSize.value
    val maxUniverses = vm.universeCount.value

    val totalChannels = 512
    val totalPages = (totalChannels + pageSize - 1) / pageSize

    var channelValues by remember { mutableStateOf(IntArray(512)) }

    val startCh = (pageIndex * pageSize + 1).coerceIn(1, 512)
    val endCh = (startCh + pageSize - 1).coerceAtMost(512)
    val visibleChannels = (startCh..endCh).toList()

    val scope = rememberCoroutineScope()

    LaunchedEffect(universe, pageIndex, pageSize) {
        while (true) {
            vm.RUni(universe, pageIndex, pageSize)
            delay(vm.DMXRefresh.longValue)
        }
    }


    LaunchedEffect(vm.wsIncoming) {
        vm.wsIncoming.collect { msg ->
            if (msg == null) return@collect
            if (!msg.startsWith("QLC+API|getChannelsValues")) return@collect

            val parts = msg.split("|")
            val newArr = channelValues.clone()

            var i = 3
            var ch = startCh

            while (i < parts.size) {
                val value = parts[i].toIntOrNull()

                if (value != null && ch in 1..512) {
                    newArr[ch - 1] = value
                }

                ch++
                i += 3
            }
            channelValues = newArr

        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Page:")
            Row {
                Button(
                    onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                    enabled = pageIndex > 0
                ) { Text("Prev") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { pageIndex = (pageIndex + 1).coerceAtMost(totalPages - 1) },
                    enabled = pageIndex < totalPages - 1
                ) { Text("Next") }
            }
        }

        Spacer(Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(),
        )
        ) {
            visibleChannels.forEach { ch ->
                val v = channelValues[ch - 1]
                VerticalFader(
                    channel = ch,
                    value = v,
                    onValueChange = { newVal ->
                        val updated = channelValues.clone()
                        updated[ch - 1] = newVal
                        channelValues = updated
                        scope.launch {
                            vm.CC(ch, newVal / 255f)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VerticalFader(channel: Int, value: Int, onValueChange: (Int) -> Unit) {

    var sliderVal by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        sliderVal = value
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {

        Text("Ch $channel", fontWeight = FontWeight.Bold)

        Slider(
            value = sliderVal / 255f,
            onValueChange = { new ->
                sliderVal = (new * 255).toInt()
                onValueChange(sliderVal)
            },
            modifier = Modifier
                .height(240.dp)
                .width(80.dp)
                .padding(vertical = 8.dp)
                .rotate(-90f)
        )

        Text("$sliderVal")
    }
}
