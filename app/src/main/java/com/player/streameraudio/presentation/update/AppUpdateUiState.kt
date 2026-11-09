package com.player.streameraudio.presentation.update

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Checking : AppUpdateUiState
    data object UpToDate : AppUpdateUiState
    data class Available(
        val versionName: String,
        val action: UpdateAction,
        val prompt: Boolean = true
    ) : AppUpdateUiState
    data class Downloading(val percent: Int?) : AppUpdateUiState
    data object ReadyToInstall : AppUpdateUiState
    data class Error(val message: String) : AppUpdateUiState
}

sealed interface UpdateAction {
    data object GooglePlay : UpdateAction
    data class DirectDownload(val url: String) : UpdateAction
}
