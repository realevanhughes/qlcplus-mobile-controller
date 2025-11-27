package com.evanh.qlc_controller

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ControlViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var controlMode = mutableStateOf(ControlMode.WEBSOCKET)
    var ip = mutableStateOf("192.168.1.1")
    var port = mutableIntStateOf(9999)

    var universeCount = mutableIntStateOf(4)
    var defaultUniverse = mutableIntStateOf(1)
    var pageSize = mutableIntStateOf(24)
    var DMXRefresh = mutableLongStateOf(20L)

    var DMXFade = mutableLongStateOf(0L)

    var useIconLabels = mutableStateOf(false)

    var useHaptics = mutableStateOf(false)

    var useSettingsPopups = mutableStateOf(true)

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { prefs ->
                ip.value = (prefs[SettingsKeys.IP] ?: ip.value)
                port.intValue = (prefs[SettingsKeys.PORT] ?: port.intValue)

                universeCount.intValue = (prefs[SettingsKeys.UNIVERSE_COUNT] ?: universeCount.intValue)
                defaultUniverse.intValue = (prefs[SettingsKeys.DEFAULT_UNIVERSE] ?: defaultUniverse.intValue)

                pageSize.intValue = (prefs[SettingsKeys.PAGE_SIZE] ?: pageSize.intValue)
                DMXRefresh.longValue = (prefs[SettingsKeys.DMX_REFRESH] ?: DMXRefresh.longValue)
                DMXFade.longValue = (prefs[SettingsKeys.DMX_FADE] ?: DMXFade.longValue)
                useIconLabels.value = (prefs[SettingsKeys.ICON_LABELS] ?: useIconLabels.value)
                useHaptics.value = (prefs[SettingsKeys.HAPTICS] ?: useHaptics.value)
                useSettingsPopups.value = (prefs[SettingsKeys.SETTINGS_POPUPS] ?: useSettingsPopups.value)
            }
        }
    }

    fun updateIp(newIp: String) = viewModelScope.launch {
        ip.value = newIp
        settingsRepository.saveIp(newIp)
    }

    fun updatePort(newPort: Int) = viewModelScope.launch {
        port.intValue = newPort
        settingsRepository.savePort(newPort)
    }

    fun updateUniverseCount(v: Int) = viewModelScope.launch {
        universeCount.intValue = v
        settingsRepository.saveUniverseCount(v)
    }

    fun updateDefaultUniverse(v: Int) = viewModelScope.launch {
        defaultUniverse.intValue = v
        settingsRepository.saveDefaultUniverse(v)
    }

    fun updatePageSize(v: Int) = viewModelScope.launch {
        pageSize.intValue = v
        settingsRepository.savePageSize(v)
    }

    fun updateDMXRefresh(v: Long) = viewModelScope.launch {
        DMXRefresh.longValue = v
        settingsRepository.saveDMXRefresh(v)
    }

    fun updateDMXFade(v: Long) = viewModelScope.launch {
        DMXFade.longValue = v
        settingsRepository.saveDMXFade(v)
    }

    fun updateIconLabels(v: Boolean) = viewModelScope.launch {
        useIconLabels.value = v
        settingsRepository.saveIconLabels(v)
    }

    fun updateHaptics(v: Boolean) = viewModelScope.launch {
        useHaptics.value = v
        settingsRepository.saveHaptics(v)
    }

    fun updateSettingsPopups(v: Boolean) = viewModelScope.launch {
        useSettingsPopups.value = v
        settingsRepository.saveSettingsPopups(v)
    }


    val globalMessage = mutableStateOf<JSONObject?>(null)

    val vcButtons = mutableStateOf<List<VcButton>>(emptyList())

    data class VcButton(
        val id: Int,
        val name: String,
        var isOn: Boolean = false
    )

    val vcWidgets = mutableStateOf<List<VcWidget>>(emptyList())
    val pendingWidgets = mutableMapOf<Int, String>()

    fun fetchVCWidgets() {
        // This function now just *starts* the process.
        // The response will be caught by the LaunchedEffect in WidgetScreen
        wsClient.send("QLC+API|getWidgetsList")
    }

    fun showMessage(type: String, message: String) {
        globalMessage.value = JSONObject().apply {
            put("type", type)
            put("message", message)
        }
    }

    fun dismissMessage() { globalMessage.value = null }

    val wsClient = QlcWebSocketClient(
        ip = { ip.value },
        port = { port.value }
    )

    val wsIncoming = wsClient.incoming
    val connected = wsClient.connected

    fun connectWebSocket() {
        if (controlMode.value == ControlMode.WEBSOCKET) {
            wsClient.connect()
        }
    }

    fun disconnectWebSocket() {
        wsClient.disconnect()
    }

    private fun dummySend(tag: String, message: String) {

    }

    suspend fun CC(channel: Int, value: Float) {
        val intValue = (value * 255f).toInt().coerceIn(0, 255)
        val msg = "CH|$channel|$intValue"

        when (controlMode.value) {
            ControlMode.NONE ->
                dummySend("DMX", msg)

            ControlMode.WEBSOCKET -> {
                wsClient.send(msg)
            }
        }
    }

    suspend fun RC(channel: Int, universe: Int) {
        val msg = "QLC+API|getChannelsValues|$universe|$channel|1"

        when (controlMode.value) {
            ControlMode.NONE ->
                dummySend("READ", msg)

            ControlMode.WEBSOCKET -> {
                wsClient.send(msg)
            }
        }
    }

    fun fetchVCButtons() {
        wsClient.send("QLC+API|getWidgetsList")
    }

    suspend fun applyToRange(
        universe: Int,
        channels: IntRange,
        value: Int,
    ) {
        val normalized = (value / 255f).coerceIn(0f, 1f)

        when (controlMode.value) {
            ControlMode.NONE -> {
                dummySend("RANGE", "Universe $universe Channels $channels → $value")
            }

            ControlMode.WEBSOCKET -> {
                channels.forEach { ch ->
                    if (ch in 1..512) {
                        CC(ch, normalized)
                        delay(DMXFade.value)
                    }
                }
            }
        }
    }

    fun cueNext(widgetID: Int) {
        val msg = "$widgetID|control|next"
        when (controlMode.value) {
            ControlMode.NONE ->
                dummySend("CUE", msg)

            ControlMode.WEBSOCKET -> {
                wsClient.send(msg)
            }
        }
    }

    suspend fun CCReset(channel: Int) {
        val msg = "QLC+API|sdResetChannel|$channel"
        when (controlMode.value) {
            ControlMode.NONE ->
                dummySend("RESET-CH", msg)

            ControlMode.WEBSOCKET -> {
                wsClient.send(msg)
            }
        }
    }

    suspend fun UniReset(universe: Int) {
        val msg = "QLC+API|sdResetUniverse|$universe"
        when (controlMode.value) {
            ControlMode.NONE ->
                dummySend("RESET-UNI", msg)

            ControlMode.WEBSOCKET -> {
                wsClient.send(msg)
            }
        }
    }

    suspend fun RUni(universe: Int, pageIndex: Int, pageSize: Int) {
        val safePageIndex = pageIndex.coerceAtLeast(0)
        val start = (safePageIndex * pageSize + 1).coerceIn(1, 512)
        val count = pageSize.coerceIn(1, 512 - start + 1)
        val msg = "QLC+API|getChannelsValues|$universe|$start|$count"

        when (controlMode.value) {
            ControlMode.NONE ->
                dummySend("READ", msg)

            ControlMode.WEBSOCKET -> {
                wsClient.send(msg)
            }
        }
    }

    suspend fun RangeReset(start: Int, end: Int, universe: Int) {
        when (controlMode.value) {
            ControlMode.NONE -> {
                dummySend("RANGE", "Reset $start to $end")
            }

            ControlMode.WEBSOCKET -> {
                val channels = 1..512
                channels.forEach { ch ->
                    if (ch in start..end) CCReset(ch)
                    delay(DMXFade.value)
                }
            }
        }
    }
}
