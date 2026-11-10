package com.hermes.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.mobile.data.SettingsStore
import com.hermes.mobile.ui.theme.HermesTheme
import com.hermes.mobile.vm.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsStore(applicationContext)
        setContent {
            HermesTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HermesApp(settings)
                }
            }
        }
    }
}

@Composable
fun HermesApp(settings: SettingsStore) {
    val vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(settings))
    val state by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(!settings.configured) }

    when {
        showSettings || !settings.configured -> SettingsScreen(
            initialUrl = settings.wsUrl,
            initialToken = settings.token,
            deviceId = state.deviceId,
            onSave = { url, token ->
                vm.saveAndConnect(url, token)
                showSettings = false
            },
        )
        else -> ChatScreen(
            state = state,
            onSend = vm::send,
            onOpenSettings = { showSettings = true },
            onAnswer = vm::answer,
        )
    }
}
