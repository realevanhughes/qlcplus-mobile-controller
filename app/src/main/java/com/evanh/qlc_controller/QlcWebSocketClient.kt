package com.evanh.qlc_controller

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*

class QlcWebSocketClient(
    private val ip: () -> String,
    private val port: () -> Int
) : WebSocketListener() {

    private val client = OkHttpClient()


    val incoming = MutableStateFlow<String?>(null)

    val connected = mutableStateOf(false)

    private var socket: WebSocket? = null

    fun connect() {
        val url = "ws://${ip()}:${port()}/qlcplusWS"
        Log.d("WS", "Connecting to $url")

        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, this)
    }

    fun disconnect() {
        connected.value = false
        socket?.close(1000, "bye")
        socket = null
    }

    fun send(msg: String) {
        socket?.send(msg)
    }

    override fun onOpen(ws: WebSocket, response: Response) {
        Log.d("WS", "WebSocket connected")
        connected.value = true
    }

    override fun onMessage(ws: WebSocket, text: String) {
        incoming.value = text
    }

    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
        Log.e("WS", "Failure: ${t.message}")
        connected.value = false
    }
}
