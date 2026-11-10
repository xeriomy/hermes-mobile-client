package com.hermes.mobile.data

import android.content.Context
import java.util.UUID

/**
 * Connection settings plus the stable per-install device identity.
 *
 * The device id determines the Hermes session (`mobile:<id>`) and the cron
 * address (`hermes send --to mobile:<id>`), so it is generated once and reused
 * forever — across restarts and updates.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("hermes_mobile", Context.MODE_PRIVATE)

    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val generated = "android-${UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    var wsUrl: String
        get() = prefs.getString(KEY_WS_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_WS_URL, value).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    val configured: Boolean
        get() = wsUrl.isNotBlank() && token.isNotBlank()

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_WS_URL = "ws_url"
        private const val KEY_TOKEN = "token"
    }
}
