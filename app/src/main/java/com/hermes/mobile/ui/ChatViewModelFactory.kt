package com.hermes.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hermes.mobile.data.SettingsStore
import com.hermes.mobile.vm.ChatViewModel

/** Small factory so the Composable can build the ViewModel with its store. */
class ChatViewModelFactory(private val settings: SettingsStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(settings) as T
}
