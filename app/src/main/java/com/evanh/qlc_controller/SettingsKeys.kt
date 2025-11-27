import androidx.datastore.preferences.core.*

object SettingsKeys {
    val IP = stringPreferencesKey("ip")
    val PORT = intPreferencesKey("port")

    val UNIVERSE_COUNT = intPreferencesKey("universe_count")
    val DEFAULT_UNIVERSE = intPreferencesKey("default_universe")

    val PAGE_SIZE = intPreferencesKey("page_size")
    val DMX_REFRESH = longPreferencesKey("dmx_refresh")
    val DMX_FADE = longPreferencesKey("dmx_fade")
    val ICON_LABELS = booleanPreferencesKey("icon_labels")
    val HAPTICS = booleanPreferencesKey("haptics")
    val SETTINGS_POPUPS = booleanPreferencesKey("settings_popups")
}