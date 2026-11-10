package com.hermes.mobile

import com.hermes.mobile.data.ChatState
import com.hermes.mobile.data.DeviceInfo
import com.hermes.mobile.data.Inbound
import com.hermes.mobile.data.Item
import com.hermes.mobile.data.helloFrame
import com.hermes.mobile.data.parseFrame
import com.hermes.mobile.data.pingFrame
import com.hermes.mobile.data.promptFrame
import com.hermes.mobile.data.textFrame
import com.hermes.mobile.data.bool
import com.hermes.mobile.data.obj
import com.hermes.mobile.data.str
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the wire protocol and the chat-list reducers — the two
 * rules that keep a streamed reply from rendering as duplicate bubbles.
 */
class ProtocolTest {

    @Test
    fun `ready frame parses`() {
        val raw = """{"t":"ready","v":1,"session":"mobile:abc","device":{"id":"abc","name":"Pixel"},"avatar_url":""}"""
        val frame = parseFrame(raw)
        assertTrue(frame is Inbound.Ready)
        frame as Inbound.Ready
        assertEquals("mobile:abc", frame.session)
        assertEquals("Pixel", frame.deviceName)
    }

    @Test
    fun `message chunks concatenate onto one bubble`() {
        val first = ChatState.applyMessage(emptyList(), Inbound.Message("m1", "Hello", edit = false, isFinal = false))
        val second = ChatState.applyMessage(first, Inbound.Message("m1", " world", edit = false, isFinal = true))
        assertEquals(1, second.size)
        assertEquals("Hello world", second[0].text)
        assertTrue(second[0].isFinal)
    }

    @Test
    fun `edit replaces the bubble and never duplicates`() {
        val draft = ChatState.applyMessage(emptyList(), Inbound.Message("m1", "draft", edit = false, isFinal = false))
        val edited = ChatState.applyMessage(draft, Inbound.Message("m1", "final", edit = true, isFinal = true))
        assertEquals(1, edited.size)
        assertEquals("final", edited[0].text)
    }

    @Test
    fun `different ids stay separate bubbles`() {
        val one = ChatState.applyMessage(emptyList(), Inbound.Message("m1", "one", edit = false, isFinal = true))
        val two = ChatState.applyMessage(one, Inbound.Message("m2", "two", edit = false, isFinal = true))
        assertEquals(2, two.size)
    }

    @Test
    fun `unknown frames are ignored not fatal`() {
        assertTrue(parseFrame("""{"t":"something.new","v":1}""") is Inbound.Unknown)
    }

    @Test
    fun `malformed frames are dropped`() {
        assertEquals(null, parseFrame("not json"))
        assertEquals(null, parseFrame("[1,2,3]"))
    }

    @Test
    fun `fatal auth error parses`() {
        val frame = parseFrame("""{"t":"error","code":"auth_failed","message":"bad token","fatal":true}""")
        assertTrue(frame is Inbound.Error)
        frame as Inbound.Error
        assertTrue(frame.fatal)
        assertEquals("auth_failed", frame.code)
    }

    @Test
    fun `prompt card parses options and command`() {
        val raw = """{"t":"prompt","id":"p1","kind":"exec_approval","text":"Run the command","options":[{"id":"once","label":"Once"}],"command":"rm -rf /tmp/x","description":"Deletes x"}"""
        val frame = parseFrame(raw)
        assertTrue(frame is Inbound.Prompt)
        frame as Inbound.Prompt
        assertEquals("exec_approval", frame.kind)
        assertEquals(listOf("once" to "Once"), frame.options)
        assertEquals("rm -rf /tmp/x", frame.command)
    }

    @Test
    fun `hello frame carries token and device`() {
        val json = helloFrame("secret", DeviceInfo(id = "android-1", name = "Pixel"))
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertEquals("hello", parsed.str("t"))
        assertEquals("secret", parsed.str("token"))
        assertEquals("android-1", parsed.obj("device")?.str("id"))
    }

    @Test
    fun `outgoing frames are well formed`() {
        assertEquals("text", Json.parseToJsonElement(textFrame("m1", "hi")).jsonObject.str("t"))
        assertEquals("m1", Json.parseToJsonElement(textFrame("m1", "hi")).jsonObject.str("id"))
        assertEquals("hi", Json.parseToJsonElement(textFrame("m1", "hi")).jsonObject.str("text"))
        assertEquals("ping", Json.parseToJsonElement(pingFrame(1L)).jsonObject.str("t"))
        assertEquals("prompt", Json.parseToJsonElement(promptFrame("p1", "once", null)).jsonObject.str("t"))
    }
}
