package com.hermes.mobile.data

/** One row in the chat list. Everything is keyed by `id` and upserted. */
enum class Role { USER, AGENT, CARD, MEDIA }

data class Item(
    val id: String,
    val role: Role,
    val text: String = "",
    val isFinal: Boolean = false,
    val edit: Boolean = false,
    // card
    val kind: String? = null,
    val options: List<Pair<String, String>> = emptyList(),
    val command: String? = null,
    val description: String? = null,
    val resolved: String? = null,
    // media
    val mediaKind: String? = null,
    val fileName: String? = null,
)

/**
 * Pure chat-list reducers.
 *
 * The two rules that matter most:
 *  - streaming chunks (`edit == false`) CONCATENATE onto the same id;
 *  - `edit == true` frames REPLACE the text of the same id.
 * Treating an edit as a new item is what produces duplicate bubbles.
 */
object ChatState {

    fun appendChunk(items: List<Item>, message: Inbound.Message): List<Item> {
        val index = items.indexOfFirst { it.id == message.id }
        if (index == -1) {
            return items + Item(id = message.id, role = Role.AGENT, text = message.text, isFinal = message.isFinal)
        }
        return items.toMutableList().apply {
            set(index, items[index].copy(text = items[index].text + message.text, isFinal = message.isFinal))
        }
    }

    fun applyEdit(items: List<Item>, message: Inbound.Message): List<Item> {
        val index = items.indexOfFirst { it.id == message.id }
        if (index == -1) {
            return items + Item(id = message.id, role = Role.AGENT, text = message.text, isFinal = message.isFinal)
        }
        return items.toMutableList().apply {
            set(index, items[index].copy(text = message.text, isFinal = message.isFinal))
        }
    }

    fun applyMessage(items: List<Item>, message: Inbound.Message): List<Item> =
        if (message.edit) applyEdit(items, message) else appendChunk(items, message)

    fun upsert(items: List<Item>, item: Item): List<Item> {
        val index = items.indexOfFirst { it.id == item.id }
        if (index == -1) return items + item
        return items.toMutableList().apply { set(index, item) }
    }
}
