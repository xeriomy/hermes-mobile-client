package com.hermes.mobile.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

enum class ConnectionState { IDLE, CONNECTING, CONNECTED, RECONNECTING, FAILED }

/**
 * OkHttp WebSocket client for the `hermes-mobile` protocol v1.
 *
 * The SDK owns hello/ready, exponential-backoff reconnect, and the fatal
 * `auth_failed` stop. Unknown frames are ignored, never fatal.
 */
class HermesClient(
    private val scope: CoroutineScope,
    private val listener: Listener,
) {
    interface Listener {
        fun onState(state: ConnectionState)
        fun onReady(session: String, deviceName: String, avatarUrl: String)
        fun onMessage(message: Inbound.Message)
        fun onTyping(stop: Boolean)
        fun onError(code: String, message: String, fatal: Boolean)
        fun onPrompt(prompt: Inbound.Prompt)
        fun onPromptDone(done: Inbound.PromptDone)
        fun onMedia(media: Inbound.Media)
        fun onAvatar(state: String)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // A chat socket is long-lived: no read timeout, keepalive via WS ping.
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val messageId = AtomicLong(1)
    private var stopped = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    fun nextMessageId(): String = "app-" + messageId.getAndIncrement().toString().padStart(12, '0')

    fun connect(url: String, token: String, device: DeviceInfo) {
        stopped = false
        open(url, token, device)
    }

    private fun open(url: String, token: String, device: DeviceInfo) {
        if (stopped) return
        listener.onState(ConnectionState.CONNECTING)
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                webSocket.send(helloFrame(token, device))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                dispatch(webSocket, parseFrame(text) ?: return)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!stopped) scheduleReconnect(url, token, device)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (stopped) return
                listener.onState(ConnectionState.RECONNECTING)
                scheduleReconnect(url, token, device)
            }
        })
    }

    private fun dispatch(webSocket: WebSocket, frame: Inbound) {
        when (frame) {
            is Inbound.Ready -> {
                listener.onState(ConnectionState.CONNECTED)
                listener.onReady(frame.session, frame.deviceName, frame.avatarUrl)
            }
            is Inbound.Message -> listener.onMessage(frame)
            is Inbound.Typing -> listener.onTyping(frame.stop)
            is Inbound.Error -> {
                listener.onError(frame.code, frame.message, frame.fatal)
                if (frame.fatal) disconnect()
            }
            is Inbound.Prompt -> listener.onPrompt(frame)
            is Inbound.PromptDone -> listener.onPromptDone(frame)
            is Inbound.Media -> listener.onMedia(frame)
            is Inbound.Avatar -> listener.onAvatar(frame.state)
            // Unknown frame types are ignored by contract — never drop the socket.
            is Inbound.Pong, is Inbound.Unknown -> Unit
        }
    }

    private fun scheduleReconnect(url: String, token: String, device: DeviceInfo) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val shift = reconnectAttempt.coerceAtMost(4)
            delay(minOf(15_000L, 1_000L * (1L shl shift)))
            reconnectAttempt += 1
            open(url, token, device)
        }
    }

    fun sendText(text: String): String {
        val id = nextMessageId()
        webSocket?.send(textFrame(id, text))
        return id
    }

    fun answerPrompt(promptId: String, optionId: String, text: String?) {
        webSocket?.send(promptFrame(promptId, optionId, text))
    }

    fun disconnect() {
        stopped = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        listener.onState(ConnectionState.IDLE)
    }
}
