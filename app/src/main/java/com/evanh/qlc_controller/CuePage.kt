package com.evanh.qlc_controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun CuePage(vm: ControlViewModel) {

    val scope = rememberCoroutineScope()

    var cueListIndex by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Cue List Controller", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = cueListIndex,
            onValueChange = { cueListIndex = it.filter { ch -> ch.isDigit() } },
            label = { Text("Cue List Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            onClick = { vm.cueNext(1) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text("GO / NEXT", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { vm.cueNext(1) },
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("PREV")
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = { vm.cueNext(1) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("STOP")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { vm.cueNext(1) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text("PLAY")
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = { vm.cueNext(1) },
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("PAUSE")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        var sliderValue by remember { mutableStateOf(0f) }
        Text("Crossfade / Slider Control", style = MaterialTheme.typography.bodyLarge)

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                vm.cueNext(1)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
