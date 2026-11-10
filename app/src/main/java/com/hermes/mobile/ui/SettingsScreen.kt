package com.hermes.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    initialUrl: String,
    initialToken: String,
    deviceId: String,
    onSave: (String, String) -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var token by remember { mutableStateOf(initialToken) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Connect to your agent", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Enter the WebSocket address and app token from your agent host.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))
        Text("WebSocket URL", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("wss://hermes.example.com/ws") },
            singleLine = true,
        )
        Text(
            "Use wss:// in production. ws:// is only for local development.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("App token", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        Text(
            "The shared secret from MOBILE_TOKEN. It goes only in the hello frame — never in the URL.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Device id", fontWeight = FontWeight.SemiBold)
        Text(
            deviceId,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Stable per install. It forms the Hermes session mobile:<id> and the cron address hermes send --to mobile:<id>.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        error?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val trimmedUrl = url.trim()
                val trimmedToken = token.trim()
                when {
                    trimmedUrl.isEmpty() -> error = "WebSocket URL is required"
                    trimmedToken.isEmpty() -> error = "Token is required"
                    !trimmedUrl.startsWith("ws://") && !trimmedUrl.startsWith("wss://") ->
                        error = "URL must start with ws:// or wss://"
                    else -> onSave(trimmedUrl, trimmedToken)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save and connect") }
    }
}
