package com.hermes.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.mobile.data.ConnectionState
import com.hermes.mobile.data.Item
import com.hermes.mobile.data.Role
import com.hermes.mobile.vm.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: UiState,
    onSend: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAnswer: (String, String, String?) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hermes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = statusLine(state),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    AvatarOrb(state.avatar)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onOpenSettings) { Text("⚙") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.fatal != null) {
                ErrorBanner(state.fatal!!)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.items.isEmpty() && state.typing.not()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No messages yet\nMessage your agent to get started.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    ItemRow(item, onAnswer)
                }
                if (state.typing) {
                    item {
                        Text(
                            "typing…",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message…") },
                    maxLines = 4,
                )
                Button(
                    onClick = {
                        onSend(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                ) { Text("→") }
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item, onAnswer: (String, String, String?) -> Unit) {
    when (item.role) {
        Role.USER -> Bubble(item.text, Alignment.CenterEnd, MaterialTheme.colorScheme.primary)
        Role.AGENT -> Bubble(item.text, Alignment.CenterStart, MaterialTheme.colorScheme.surface)
        Role.MEDIA -> Bubble("${item.mediaKind ?: "file"} · ${item.fileName ?: ""}", Alignment.CenterStart, MaterialTheme.colorScheme.surface)
        Role.CARD -> CardItem(item, onAnswer)
    }
}

@Composable
private fun Bubble(text: String, alignment: Alignment, background: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = background,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(text, Modifier.padding(12.dp), fontSize = 15.sp)
        }
    }
}

@Composable
private fun CardItem(item: Item, onAnswer: (String, String, String?) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.text, fontSize = 15.sp)
            item.command?.let {
                Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            item.description?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            item.options.forEach { (id, label) ->
                Button(
                    onClick = { onAnswer(item.id.removePrefix("card-"), id, null) },
                    modifier = Modifier.padding(vertical = 2.dp),
                    enabled = item.resolved == null,
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun AvatarOrb(state: String) {
    val scale = if (state == "speaking") 1.2f else if (state == "thinking") 1.12f else 1f
    val alpha = if (state == "idle") 0.7f else 1f
    Box(
        Modifier
            .size(32.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onError,
            fontSize = 13.sp,
            modifier = Modifier.padding(10.dp),
        )
    }
}

private fun statusLine(state: UiState): String = when {
    state.fatal != null -> "no access"
    state.connection == ConnectionState.CONNECTED -> if (state.typing) "typing…" else "agent online"
    state.connection == ConnectionState.CONNECTING -> "connecting…"
    state.connection == ConnectionState.RECONNECTING -> "reconnecting…"
    state.connection == ConnectionState.FAILED -> "no access"
    else -> "idle"
}
