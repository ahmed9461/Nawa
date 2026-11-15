package com.ahmed9461.nawa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmed9461.nawa.NawaViewModel

@Composable
fun NawaApp(viewModel: NawaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf("home") }
    var chatId by rememberSaveable { mutableStateOf("") }
    val savedScreens = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val home = { destination = "home" }
    BackHandler(destination != "home") { home() }
    LaunchedEffect(state.notice) {
        state.notice?.let { snackbar.showSnackbar(it); viewModel.clearNotice() }
    }
    Box(Modifier.fillMaxSize()) {
        savedScreens.SaveableStateProvider(if (destination == "chat") "chat:$chatId" else destination) {
            when (destination) {
                "models" -> ModelsScreen(state, home, viewModel::importModel, viewModel::loadModel,
                    viewModel::unloadModel, viewModel::deleteModel, viewModel::cancelImport, viewModel::cancelModelOperation)
                "settings" -> SettingsScreen(state, home, viewModel::updateSettings,
                    viewModel::exportBackup, viewModel::importBackup)
                "chat" -> ChatScreen(state.threads.find { it.id == chatId }, state, home,
                    { destination = "models" }, { viewModel.sendMessage(chatId, it) }, viewModel::stopGeneration,
                    { viewModel.regenerate(chatId) })
                else -> HomeScreen(state,
                    onNewChat = { viewModel.createThread()?.let { chatId = it; destination = "chat" } },
                    onOpenChat = { chatId = it; destination = "chat" },
                    onModels = { destination = "models" }, onSettings = { destination = "settings" },
                    onDelete = viewModel::deleteThread, onRename = viewModel::renameThread,
                    onRetryStorage = viewModel::reloadChats)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp))
        state.error?.let { ErrorDialog(it, viewModel::clearError) }
    }
}
