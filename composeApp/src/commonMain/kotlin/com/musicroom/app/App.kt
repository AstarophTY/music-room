package com.musicroom.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import com.musicroom.app.theme.AppTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.launch

interface SettingsStore {
    var backendUrl: String
}

@Composable
fun App(
    settings: SettingsStore,
    apiFactory: (baseUrl: String) -> MusicRoomApi,
) {
    var backendUrl by remember { mutableStateOf(settings.backendUrl) }
    var status by remember { mutableStateOf("Not tested") }
    val scope = rememberCoroutineScope()

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.safeDrawingPadding().imePadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Music Room", style = MaterialTheme.typography.headlineMedium)

                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Backend URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(onClick = {
                    settings.backendUrl = backendUrl
                    status = "Connecting..."
                    scope.launch {
                        val api = apiFactory(backendUrl)
                        status = runCatching { api.health() }
                            .fold(
                                onSuccess = { "Server: ${it.status}, database: ${it.database}" },
                                onFailure = { "Error: ${it.message}" },
                            )
                        api.close()
                    }
                }) {
                    Text("Save and test")
                }

                Text(status)
            }
        }
    }
}
