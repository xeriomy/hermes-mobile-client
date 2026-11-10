package com.hermes.mobile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.data.ChatState
import com.hermes.mobile.data.ConnectionState
import com.hermes.mobile.data.DeviceInfo
import com.hermes.mobile.data.HermesClient
import com.hermes.mobile.data.Inbound
import com.hermes.mobile.data.Item
import com.hermes.mobile.data.Role
import com.hermes.mobile.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val connection: ConnectionState = ConnectionState.IDLE,
    val fatal: String? = null,
    val transientError: String? = null,
    val session: String = "",
    val items: List<Item> = emptyList(),
    val typing: Boolean = false,
    val avatar: String = "idle",
    val deviceId: String = "",
)

/**
 * Chat state holder. One list of [Item]s keyed by `id`; streaming chunks
 * concatenate and `edit:true` frames replace, so a streamed reply can never
 * produce a duplicate bubble.
 */
class ChatViewModel(private val settings: SettingsStore) : ViewModel(), HermesClient.Listener {

    private val _state = MutableStateFlow(UiState(deviceId = settings.deviceId()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var client: HermesClient? = null

    val configured: Boolean get() = settings.configured

    fun connect() {
        if (client != null) return
        val created = HermesClient(viewModelScope, this)
        client = created
        created.connect(
            settings.wsUrl,
            settings.token,
            DeviceInfo(id = settings.deviceId(), name = "Android"),
        )
    }

    fun saveAndConnect(url: String, token: String) {
        settings.wsUrl = url.trim()
        settings.token = token.trim()
        connect()
    }

    fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val id = client?.sendText(body) ?: return
        _state.update { current ->
            current.copy(items = ChatState.upsert(current.items, Item(id = "u-$id", role = Role.USER, text = body)))
        }
    }

    fun answer(promptId: String, optionId: String, text: String?) {
        client?.answerPrompt(promptId, optionId, text)
        _state.update { current ->
            current.copy(
                items = ChatState.upsert(
                    current.items,
                    Item(id = "card-$promptId", role = Role.CARD, resolved = optionId),
                ),
            )
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
    }

    // ── HermesClient.Listener ──────────────────────────────────────────────

    override fun onState(state: ConnectionState) {
        _state.update { it.copy(connection = state) }
    }

    override fun onReady(session: String, deviceName: String, avatarUrl: String) {
        _state.update { it.copy(session = session) }
    }

    override fun onMessage(message: Inbound.Message) {
        _state.update { it.copy(typing = false, items = ChatState.applyMessage(it.items, message)) }
    }

    override fun onTyping(stop: Boolean) {
        _state.update { it.copy(typing = !stop) }
    }

    override fun onError(code: String, message: String, fatal: Boolean) {
        _state.update {
            it.copy(
                fatal = if (fatal) message else it.fatal,
                transientError = if (fatal) it.transientError else message,
            )
        }
    }

    override fun onPrompt(prompt: Inbound.Prompt) {
        _state.update { current ->
            current.copy(
                items = ChatState.upsert(
                    current.items,
                    Item(
                        id = "card-${prompt.id}",
                        role = Role.CARD,
                        text = prompt.text,
                        kind = prompt.kind,
                        options = prompt.options,
                        command = prompt.command,
                        description = prompt.description,
                    ),
                ),
            )
        }
    }

    override fun onPromptDone(done: Inbound.PromptDone) {
        _state.update { current ->
            current.copy(
                items = ChatState.upsert(
                    current.items,
                    Item(
                        id = "card-${done.id}",
                        role = Role.CARD,
                        resolved = done.optionId ?: if (done.expired) "expired" else "closed",
                    ),
                ),
            )
        }
    }

    override fun onMedia(media: Inbound.Media) {
        _state.update { current ->
            current.copy(
                items = ChatState.upsert(
                    current.items,
                    Item(
                        id = "media-${media.id}",
                        role = Role.MEDIA,
                        text = media.caption,
                        mediaKind = media.kind,
                        fileName = media.fileName,
                    ),
                ),
            )
        }
    }

    override fun onAvatar(state: String) {
        _state.update { it.copy(avatar = state) }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
