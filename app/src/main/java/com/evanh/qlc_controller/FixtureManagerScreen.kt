package com.evanh.qlc_controller

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class FixtureAttribute(
    val name: String,
    val width: Int
)

data class FixtureType(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val attributes: List<FixtureAttribute>
)

data class FixtureInstance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val typeId: String,
    val universe: Int,
    val startAddress: Int,
    val name: String
) {
    fun channelRange(type: FixtureType): IntRange {
        val total = type.attributes.sumOf { it.width }
        return startAddress until (startAddress + total)
    }
}

@Composable
fun FixtureManagerScreen(vm: ControlViewModel) {

    var showAddType by remember { mutableStateOf(false) }
    var showAddFixture by remember { mutableStateOf(false) }
    var selectedFixtureId by remember { mutableStateOf<String?>(null) }
    var universe by remember { mutableStateOf(vm.defaultUniverse.intValue) }

    var channelValues by remember { mutableStateOf(IntArray(512)) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    var groupValue by remember { mutableStateOf(0) }

    var fadeOut by remember { mutableStateOf(false) }

    val selectedFixture = selectedFixtureId?.let { id ->
        vm.fixtures.firstOrNull { it.id == id }
    }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showAddType = true }) {
                Text("New Fixture Type")
            }
            Button(onClick = { showAddFixture = true }) {
                Text("Add Fixture")
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Fixtures",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))
        val alpha by animateFloatAsState(if (fadeOut) 0f else 1f)
        Box(modifier = Modifier
            .weight(1f)
            .alpha(alpha)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.fixtures) { fx ->
                    val isSel = selected.contains(fx.id)
                    val fixtureTypesById = vm.fixtureTypes.associateBy { it.id }
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
                                    if (isSel) selected - fx.id
                                    else selected + fx.id
                            }
                            .padding(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(fx.name, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(fixtureTypesById[fx.typeId]?.name ?: "Unknown")
                        }
                    }
                }
            }
        }
        FixtureFaders(
            selected = selected,
            groupValue = groupValue,
            onGroupValueChange = { groupValue = it },
            vm = vm,
            fadeOut = fadeOut,
            onFadeOut = { fadeOut = it }
        )
    }

    if (showAddType) {
        AddFixtureTypeDialog(
            onDismiss = { showAddType = false },
            onSave = { name, attrs ->
                vm.addFixtureType(name, attrs)
                showAddType = false
            }
        )
    }

    if (showAddFixture) {
        AddFixtureDialog(
            vm = vm,
            onDismiss = { showAddFixture = false },
            onSave = { typeId, universe, addr, name ->
                vm.addFixtureInstance(typeId, universe, addr, name)
                showAddFixture = false
            }
        )
    }

    if (selectedFixture != null) {
        FixtureParameterDialog(
            vm = vm,
            fixture = selectedFixture,
            onDismiss = { selectedFixtureId = null }
        )
    }
}

@Composable
fun FixtureCard(
    fixture: FixtureInstance,
    type: FixtureType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(type.name, fontWeight = FontWeight.Bold)
            Text("Universe: ${fixture.universe}")
            Text("Address: ${fixture.startAddress}")
            Text("Channels: ${type.attributes.sumOf { it.width }}")
        }
    }
}

@Composable
fun AddFixtureTypeDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<FixtureAttribute>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var attributeName by remember { mutableStateOf("") }
    var attributeWidth by remember { mutableStateOf("1") }

    val attributes = remember { mutableStateListOf<FixtureAttribute>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Fixture Type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Type Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attributeName,
                        onValueChange = { attributeName = it },
                        label = { Text("Attribute") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = attributeWidth,
                        onValueChange = { attributeWidth = it },
                        label = { Text("Width") },
                        modifier = Modifier.width(80.dp)
                    )
                    Button(onClick = {
                        if (attributeName.isNotBlank()) {
                            attributes += FixtureAttribute(
                                attributeName,
                                attributeWidth.toIntOrNull() ?: 1
                            )
                            attributeName = ""
                            attributeWidth = "1"
                        }
                    }) {
                        Text("Add")
                    }
                }

                attributes.forEach {
                    Text("${it.name} (${it.width})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, attributes.toList()) },
                enabled = name.isNotEmpty() && attributes.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddFixtureDialog(
    vm: ControlViewModel,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, String) -> Unit
) {
    var selectedTypeName by remember {
        mutableStateOf(vm.fixtureTypes.firstOrNull()?.name ?: "")
    }

    val selectedTypeId: String = vm.fixtureTypes
        .firstOrNull { it.name == selectedTypeName }?.id ?: ""

    var universe by remember { mutableStateOf("1") }
    var address by remember { mutableStateOf("1") }
    var name by remember { mutableStateOf("New Fixture") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Fixture") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                DropdownMenuSwitcher(
                    current = selectedTypeName,
                    options = vm.fixtureTypes.map { it.name }
                ) {
                    selectedTypeName = it
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )

                OutlinedTextField(
                    value = universe,
                    onValueChange = { universe = it },
                    label = { Text("Universe") }
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Start Address") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(selectedTypeId, universe.toInt(), address.toInt(), name)
            }) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
@Composable
fun FixtureParameterDialog(
    vm: ControlViewModel,
    fixture: FixtureInstance,
    onDismiss: () -> Unit
) {
    val type = vm.getType(fixture.typeId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Fixture Parameters: ${type.name}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                type.attributes.forEach {
                    Text("${it.name} (${it.width} ch)")
                }

                val range = fixture.channelRange(type)
                Text("Universe: ${fixture.universe}")
                Text("Channels: $range")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun FixtureFaders(
    selected: Set<String>,
    groupValue: Int,
    onGroupValueChange: (Int) -> Unit,
    vm: ControlViewModel,
    fadeOut: Boolean,
    onFadeOut: (Boolean) -> Unit,
) {
    if (selected.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    var activeEffect by remember { mutableStateOf(FX.None) }

    var all_attributes = vm.getAttributes()

    val fixtureTypesById = remember(vm.fixtureTypes) {
        vm.fixtureTypes.associateBy { it.id }
    }

    val scope = rememberCoroutineScope()

    fun sendDmxValue(value: Float, byteValue: Byte) {
        scope.launch {
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
                    "Fixture control (${selected.size} channels)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    expanded = !expanded
                    onFadeOut((!fadeOut))
                    all_attributes = vm.getAttributes()
                    Log.d("debug", "all att: ${all_attributes.toString()}")
                }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            var globalSliderValue by remember() { mutableStateOf(0f) }
            Slider(
                value = globalSliderValue,
                onValueChange = { newValue ->
                    globalSliderValue = newValue

                    val newVal255 = (newValue * 255f)

                    for (fx in vm.fixtures) {
                        for (att in all_attributes) {
                            val type = fixtureTypesById[fx.typeId] ?: continue
                            val index = type.attributes.indexOf(att)
                            if (index == -1) continue
                            val dmxChannel = fx.startAddress + index
                            scope.launch {
                                vm.CC(dmxChannel, newVal255)
                            }
                        }
                    }
                }
            )

            AnimatedVisibility(visible = expanded) {
                Column {

                    Spacer(Modifier.height(12.dp))

                    for (att in all_attributes) {

                        var sliderValue by remember(att) { mutableStateOf(0f) }

                        Text(att.name)

                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                sliderValue = newValue

                                val newVal255 = (newValue * 255f)

                                for (fx in vm.fixtures) {
                                    val type = fixtureTypesById[fx.typeId] ?: continue
                                    val index = type.attributes.indexOf(att)
                                    if (index == -1) continue
                                    val dmxChannel = fx.startAddress + index
                                    scope.launch {
                                        vm.CC(dmxChannel, newVal255)
                                    }
                                }
                            },
                            colors =
                                when (att.name) {
                                    "Red" -> SliderDefaults.colors(Color(0xFFF44336))
                                    "Green" -> SliderDefaults.colors(Color(0xFF4CAF50))
                                    "Blue" -> SliderDefaults.colors(Color(0xFF2196F3))
                                    else -> {
                                        SliderDefaults.colors(MaterialTheme.colorScheme.primaryContainer)
                                    }
                                }
                        )

                    }
                }
                }
            }
        }
}
