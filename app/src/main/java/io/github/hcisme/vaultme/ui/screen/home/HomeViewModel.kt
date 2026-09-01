package io.github.hcisme.vaultme.ui.screen.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import io.github.hcisme.vaultme.repository.WebDavRepository
import io.github.hcisme.vaultme.room.appDatabase
import io.github.hcisme.vaultme.room.credentialDao
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    var isRefreshing by mutableStateOf(false)
        private set
    private var hasLoadedOnce = false
    private val webDavRepository = WebDavRepository.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val credentialsFlow: StateFlow<List<CredentialEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            application.credentialDao.getAllCredentials()
        } else {
            application.credentialDao.searchCredentials(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadInitial() {
        if (hasLoadedOnce) return
        hasLoadedOnce = true
        pull()
    }

    fun refresh() {
        pull()
    }

    fun deleteCredential(entity: CredentialEntity) {
        viewModelScope.launch {
            application.credentialDao.deleteCredential(entity)
            runCatching { webDavRepository.deleteCredential(entity.uuid) }
        }
    }

    private fun pull() {
        if (isRefreshing) return

        viewModelScope.launch {
            isRefreshing = true
            try {
                val remote = webDavRepository.downloadCredentials()
                if (remote.isEmpty()) return@launch

                val dao = application.credentialDao
                val localByUuid = dao.getAllCredentialsOnce().associateBy { it.uuid }

                val toInsert = mutableListOf<CredentialEntity>()
                val toUpdate = mutableListOf<CredentialEntity>()
                for (entity in remote) {
                    if (entity.uuid.isBlank()) continue
                    val local = localByUuid[entity.uuid]
                    if (local == null) {
                        toInsert.add(entity)
                    } else if (entity.updatedAt > local.updatedAt) {
                        toUpdate.add(
                            local.copy(
                                platform = entity.platform,
                                account = entity.account,
                                password = entity.password,
                                updatedAt = entity.updatedAt
                            )
                        )
                    }
                }

                application.appDatabase.withTransaction {
                    if (toInsert.isNotEmpty()) dao.insertCredentials(toInsert)
                    if (toUpdate.isNotEmpty()) dao.updateCredentials(toUpdate)
                }
            } finally {
                isRefreshing = false
            }
        }
    }
}
