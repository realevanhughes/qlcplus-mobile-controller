package com.evanh.qlc_controller

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.collections.setOf
import kotlin.math.sin

@Composable
fun DmxMonitorScreen(vm: ControlViewModel) {

    var pageIndex by remember { mutableStateOf(0) }
    var universe by remember { mutableStateOf(vm.defaultUniverse.intValue) }
    val pageSize = vm.pageSize.intValue

    val totalChannels = 512
    val totalPages = (totalChannels + pageSize - 1) / pageSize

    var channelValues by remember { mutableStateOf(IntArray(512)) }
    var selected by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }

    var groupValue by remember { mutableStateOf(0) }

    var fadeOut by remember { mutableStateOf(false) }

    LaunchedEffect(selected) {
        if (selected.isNotEmpty()) {
            val avg = selected.map { (uni, ch) ->
                channelValues[(ch - 1)]
            }.average().toInt()
            groupValue = avg
        }
    }

    val startCh = (pageIndex * pageSize + 1).coerceIn(1, 512)
    val endCh = (startCh + pageSize - 1).coerceAtMost(512)
    val visibleChannels = (startCh..endCh).toList()

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

    Column(Modifier
        .fillMaxSize()
        .padding(12.dp)) {

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Page:", fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                selected = selected + visibleChannels.map { ch -> universe to ch }
            }) {
                Text("Select All (Page)")
            }

            Button(onClick = { selected = emptySet() }) {
                Text("Clear Selection")
            }
        }

        Spacer(Modifier.height(16.dp))

        val alpha by animateFloatAsState(if (fadeOut) 0f else 1f)
        Box(modifier = Modifier.weight(1f).alpha(alpha)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleChannels) { ch ->
                val value = channelValues[(ch - 1)]
                val isSel = selected.contains(universe to ch)
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            2.dp,
                            if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable {
                            selected =
                                if (isSel) selected - (universe to ch)
                                else selected + (universe to ch)
                        }
                        .padding(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("U$universe Ch $ch", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = value / 255f,
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        Text("$value")
                    }
                }
            }
        }
        }
        GroupFaderWithFX(
            selected = selected,
            groupValue = groupValue,
            onGroupValueChange = { groupValue = it },
            vm = vm,
            channelValues = channelValues,
            fadeOut = fadeOut,
            onFadeOut = { fadeOut = it }
        )

    }
}
enum class FX { None, Strobe, Pulse, Chase, Wave }
@Composable
fun GroupFaderWithFX(
    selected: Set<Pair<Int, Int>>,
    groupValue: Int,
    onGroupValueChange: (Int) -> Unit,
    vm: ControlViewModel,
    channelValues: IntArray,
    fadeOut: Boolean,
    onFadeOut: (Boolean) -> Unit,
) {
    if (selected.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    var activeEffect by remember { mutableStateOf(FX.None) }

    val scope = rememberCoroutineScope()
    val flashInteractionSource = remember { MutableInteractionSource() }

    fun sendDmxValue(value: Float, byteValue: Byte) {
        scope.launch {
            for ((_, ch) in selected) {
                vm.CC(ch, value)
                if (ch in 1..channelValues.size) channelValues[ch - 1] = byteValue.toInt()
                delay(vm.DMXRefresh.longValue)
            }
        }
    }

    LaunchedEffect(flashInteractionSource) {
        flashInteractionSource.interactions.collect { inter ->
            when (inter) {
                is PressInteraction.Press -> sendDmxValue(1f, 255.toByte())
                is PressInteraction.Release, is PressInteraction.Cancel ->
                    sendDmxValue(0f, 0.toByte())
            }
        }
    }

    LaunchedEffect(activeEffect) {
        when (activeEffect) {

            FX.Strobe -> while (activeEffect == FX.Strobe) {
                sendDmxValue(1f, 255.toByte())
                delay(60)
                sendDmxValue(0f, 0.toByte())
                delay(60)
            }

            FX.Pulse -> {
                var up = true
                while (activeEffect == FX.Pulse) {
                    val range = if (up) (0..255) else (255 downTo 0)
                    for (i in range step 5) {
                        sendDmxValue(i / 255f, i.toByte())
                        delay(12)
                    }
                    up = !up
                }
            }

            FX.Chase -> {
                var index = 0
                while (activeEffect == FX.Chase) {
                    selected.forEachIndexed { i, (_, ch) ->
                        val value = if (i == index) 1f else 0f
                        vm.CC(ch, value)
                        if (ch in 1..channelValues.size) channelValues[ch - 1] = if (value == 1f) 255 else 0
                    }
                    index = (index + 1) % selected.size
                    delay(80)
                }
            }

            FX.Wave -> {
                var t = 0.0
                while (activeEffect == FX.Wave) {
                    selected.forEachIndexed { i, (_, ch) ->
                        val v = ((sin(t + i * 0.4) + 1) / 2).toFloat()
                        vm.CC(ch, v)
                        channelValues[ch - 1] = (v * 255).toInt()
                    }
                    t += 0.25
                    delay(25)
                }
            }

            FX.None -> {}
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Group Fader (${selected.size} Channels)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    expanded = !expanded
                    onFadeOut((!fadeOut))
                }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            Slider(
                value = groupValue / 255f,
                onValueChange = { f ->
                    val newVal = (f * 255).toInt().coerceIn(0, 255)
                    onGroupValueChange(newVal)

                    scope.launch {
                        for ((_, ch) in selected) {
                            vm.CC(ch, newVal / 255f)
                            channelValues[ch - 1] = newVal
                            delay(vm.DMXFade.longValue)
                        }
                    }
                }
            )

            AnimatedVisibility(visible = expanded) {
                Column {

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                        Button(
                            onClick = {},
                            interactionSource = flashInteractionSource,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Bolt, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Flash")
                        }

                        Button(
                            onClick = { sendDmxValue(0f, 0.toByte()) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cancel, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Zero")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    for ((_, ch) in selected) {
                                        vm.CCReset(ch)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Clear")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Light Effects",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FXButton(
                            name = "Strobe",
                            effect = FX.Strobe,
                            icon = Icons.Default.TipsAndUpdates,
                            activeEffect = activeEffect,
                            onClick = { activeEffect = it },
                            modifier = Modifier.weight(1f)
                        )

                        FXButton(
                            name = "Pulse",
                            effect = FX.Pulse,
                            icon = Icons.Default.Favorite,
                            activeEffect = activeEffect,
                            onClick = { activeEffect = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FXButton(
                            name = "Chase",
                            effect = FX.Chase,
                            icon = Icons.Default.Route,
                            activeEffect = activeEffect,
                            onClick = { activeEffect = it },
                            modifier = Modifier.weight(1f)
                        )
                        FXButton(
                            name = "Wave",
                            effect = FX.Wave,
                            icon = Icons.Default.Water,
                            activeEffect = activeEffect,
                            onClick = { activeEffect = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            activeEffect = FX.None
                            sendDmxValue(0f, 0.toByte())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop Effects")
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        border = BorderStroke(1.dp, Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Value: $groupValue",
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FXButton(
    name: String,
    effect: FX,
    icon: ImageVector,
    activeEffect: FX,
    onClick: (FX) -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = { onClick(effect) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors =
            if (activeEffect == effect)
                ButtonDefaults.filledTonalButtonColors(
                    MaterialTheme.colorScheme.primaryContainer
                )
            else
                ButtonDefaults.filledTonalButtonColors()
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(4.dp))
        Text(name)
    }
}
@Composable
fun DropdownMenuSwitcher(current: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) { Text(current) }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelected(opt)
                    }
                )
            }
        }
    }
}
