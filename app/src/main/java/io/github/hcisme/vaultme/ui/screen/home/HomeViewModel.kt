package io.github.hcisme.vaultme.ui.screen.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.hcisme.vaultme.room.credentialDao

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    var searchQuery by mutableStateOf("")
        private set

    val credentialsFlow = application.credentialDao.getAllCredentials()

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }
}

