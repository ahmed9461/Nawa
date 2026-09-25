package com.ahmed9461.nawa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmed9461.nawa.NawaViewModel

private sealed interface Destination {
    data object Home : Destination
    data object Models : Destination
    data object Settings : Destination
    data class Chat(val id: String) : Destination
}

@Composable
fun NawaApp(viewModel: NawaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf<Destination>(Destination.Home) }

    BackHandler(destination !is Destination.Home) {
        destination = Destination.Home
    }

    Box(Modifier.fillMaxSize()) {
        when (val screen = destination) {
            Destination.Home -> HomeScreen(
                state = state,
                onNewChat = {
                    destination = Destination.Chat(viewModel.createThread())
                },
                onOpenChat = { destination = Destination.Chat(it) },
                onModels = { destination = Destination.Models },
                onSettings = { destination = Destination.Settings },
                onDelete = viewModel::deleteThread,
            )

            Destination.Models -> ModelsScreen(
                state = state,
                onBack = { destination = Destination.Home },
                onImport = viewModel::importModel,
                onLoad = viewModel::loadModel,
                onUnload = viewModel::unloadModel,
                onDelete = viewModel::deleteModel,
            )

            Destination.Settings -> SettingsScreen(
                state = state,
                onBack = { destination = Destination.Home },
                onSave = viewModel::updateSettings,
            )

            is Destination.Chat -> ChatScreen(
                thread = state.threads.firstOrNull { it.id == screen.id },
                state = state,
                onBack = { destination = Destination.Home },
                onModels = { destination = Destination.Models },
                onSend = { viewModel.sendMessage(screen.id, it) },
                onStop = viewModel::stopGeneration,
            )
        }

        state.error?.let { error ->
            ErrorDialog(error = error, onDismiss = viewModel::clearError)
        }
    }
}
