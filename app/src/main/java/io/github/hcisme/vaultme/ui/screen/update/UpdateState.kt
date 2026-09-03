package io.github.hcisme.vaultme.ui.screen.update

sealed interface UpdateState {
    data object Checking : UpdateState
    data class HasUpdate(val tagName: String, val body: String, val downloadUrl: String) :
        UpdateState

    data object Downloading : UpdateState
    data object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}
