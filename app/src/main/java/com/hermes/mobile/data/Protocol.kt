package com.hermes.mobile.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The `hermes-mobile` protocol v1: one JSON object per WebSocket frame.
 *
 * A frame is `{t, ...}` where `t` selects the shape. Unknown frame types are
 * reported as [Inbound.Unknown] and must be ignored, never fatal.
 */
object Proto {
    const val VERSION = 1
    const val MAX_FRAME_BYTES = 16 * 1024 * 1024
    const val MAX_TEXT_CHARS = 12000

    const val C_HELLO = "hello"
    const val C_TEXT = "text"
    const val C_PING = "ping"
    const val C_PROMPT = "prompt"

    const val S_READY = "ready"
    const val S_MESSAGE = "message"
    const val S_TYPING = "typing"
    const val S_ERROR = "error"
    const val S_PONG = "pong"
    const val S_PROMPT = "prompt"
    const val S_PROMPT_DONE = "prompt_done"
    const val S_MEDIA = "media"
    const val S_AVATAR = "avatar"
}

/** Stable per-install device identity (the Hermes session is `mobile:<id>`). */
data class DeviceInfo(
    val id: String,
    val name: String,
    val platform: String = "android",
    val appVersion: String = "0.1.0",
)

/** Incoming frames, normalised. */
sealed class Inbound {
    data class Ready(val session: String, val deviceName: String, val avatarUrl: String) : Inbound()
    data class Message(val id: String, val text: String, val edit: Boolean, val isFinal: Boolean) : Inbound()
    data class Typing(val stop: Boolean) : Inbound()
    data class Error(val code: String, val message: String, val fatal: Boolean) : Inbound()
    data class Pong(val ts: Long) : Inbound()

    /** A prompt card (exec_approval / slash_confirm / clarify). */
    data class Prompt(
        val id: String,
        val kind: String,
        val text: String,
        val options: List<Pair<String, String>>,
        val command: String?,
        val description: String?,
    ) : Inbound()

    data class PromptDone(val id: String, val optionId: String?, val expired: Boolean) : Inbound()
    data class Media(
        val id: String,
        val kind: String,
        val mime: String,
        val fileName: String,
        val caption: String,
        val data: String?,
        val url: String?,
    ) : Inbound()

    data class Avatar(val state: String) : Inbound()
    data class Unknown(val type: String) : Inbound()
}

/**
 * Frame parsing. `text` may be null at a segment boundary; every other missing
 * field is a default, so a partial frame still renders.
 */
fun parseFrame(raw: String): Inbound? {
    if (raw.length > Proto.MAX_FRAME_BYTES) return null
    val frame = try {
        Json.parseToJsonElement(raw).jsonObject
    } catch (_: Exception) {
        return null
    }
    return when (frame.str("t")) {
        Proto.S_READY -> Inbound.Ready(
            session = frame.str("session") ?: "",
            deviceName = frame.obj("device")?.str("name") ?: "",
            avatarUrl = frame.str("avatar_url") ?: "",
        )
        Proto.S_MESSAGE -> Inbound.Message(
            id = frame.str("id") ?: return null,
            text = frame.str("text") ?: "",
            edit = frame.bool("edit") ?: false,
            isFinal = frame.bool("final") ?: false,
        )
        Proto.S_TYPING -> Inbound.Typing(stop = frame.bool("stop") ?: false)
        Proto.S_ERROR -> Inbound.Error(
            code = frame.str("code") ?: "internal_error",
            message = frame.str("message") ?: "error",
            fatal = frame.bool("fatal") ?: false,
        )
        Proto.S_PONG -> Inbound.Pong(frame.long("ts") ?: 0L)
        Proto.S_PROMPT -> Inbound.Prompt(
            id = frame.str("id") ?: return null,
            kind = frame.str("kind") ?: "",
            text = frame.str("text") ?: "",
            options = frame.array("options").mapNotNull { element ->
                val option = element as? JsonObject ?: return@mapNotNull null
                (option.str("id") ?: return@mapNotNull null) to (option.str("label") ?: "")
            },
            command = frame.str("command"),
            description = frame.str("description"),
        )
        Proto.S_PROMPT_DONE -> Inbound.PromptDone(
            id = frame.str("id") ?: return null,
            optionId = frame.str("option_id"),
            expired = frame.bool("expired") ?: false,
        )
        Proto.S_MEDIA -> Inbound.Media(
            id = frame.str("id") ?: return null,
            kind = frame.str("kind") ?: "document",
            mime = frame.str("mime") ?: "application/octet-stream",
            fileName = frame.str("filename") ?: "file",
            caption = frame.str("caption") ?: "",
            data = frame.str("data"),
            url = frame.str("url"),
        )
        Proto.S_AVATAR -> Inbound.Avatar(state = frame.str("state") ?: "idle")
        else -> Inbound.Unknown(frame.str("t") ?: "")
    }
}

fun helloFrame(token: String, device: DeviceInfo): String = buildJsonObject {
    put("t", Proto.C_HELLO)
    put("token", token)
    putJsonObject("device") {
        put("id", device.id)
        put("name", device.name)
        put("platform", device.platform)
        put("app_version", device.appVersion)
    }
    putJsonObject("user") { }
}.toString()

fun textFrame(id: String, text: String): String = buildJsonObject {
    put("t", Proto.C_TEXT)
    put("id", id)
    put("text", text)
}.toString()

fun pingFrame(ts: Long): String = buildJsonObject {
    put("t", Proto.C_PING)
    put("ts", ts)
}.toString()

fun promptFrame(promptId: String, optionId: String, text: String?): String = buildJsonObject {
    put("t", Proto.C_PROMPT)
    put("prompt_id", promptId)
    put("option_id", optionId)
    if (text != null) put("text", text)
}.toString()

// ── JSON helpers ──────────────────────────────────────────────────────────

fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.booleanOrNull

fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.longOrNull

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.array(key: String): List<kotlinx.serialization.json.JsonElement> =
    this[key] as? kotlinx.serialization.json.JsonArray ?: emptyList()
