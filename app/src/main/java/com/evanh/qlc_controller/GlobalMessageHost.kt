package com.evanh.qlc_controller

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun GlobalMessageHost(vm: ControlViewModel) {
    val msgObj = vm.globalMessage.value ?: return

    val type = msgObj.optString("type", "Info")
    val message = msgObj.optString("message", "")

    AlertDialog(
        onDismissRequest = { vm.dismissMessage() },
        title = { Text(type) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { vm.dismissMessage() }) {
                Text("OK")
            }
        }
    )
}

