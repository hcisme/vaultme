package io.github.hcisme.vaultme.ui.screen.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.hcisme.vaultme.datastore.SettingsDataStore
import io.github.hcisme.vaultme.repository.WebDavRepository
import io.github.hcisme.vaultme.room.credentialDao
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    var isRefreshing by mutableStateOf(false)
        private set
    private val webDavRepository = WebDavRepository(
        settingsStore = SettingsDataStore(application)
    )

    // 搜索词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val credentialsFlow: Flow<List<CredentialEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            application.credentialDao.getAllCredentials()
        } else {
            application.credentialDao.searchCredentials(query)
        }
    }

    /**
     * 提交搜索（键盘搜索键 / 清空输入框时触发）
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 手动刷新。
     */
    fun refresh() {
        pull()
    }

    /**
     * 删除凭据：先删本地，再删云端（失败不阻塞）。
     */
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
                for (entity in remote) {
                    if (entity.uuid.isBlank()) continue
                    val local = application.credentialDao.getCredentialByUuid(entity.uuid)
                    if (local == null) {
                        application.credentialDao.insertCredential(entity)
                    } else if (entity.updatedAt > local.updatedAt) {
                        // 保留本地 id，用远端内容覆盖
                        application.credentialDao.updateCredential(
                            local.copy(
                                platform = entity.platform,
                                account = entity.account,
                                password = entity.password,
                                updatedAt = entity.updatedAt
                            )
                        )
                    }
                }
            } finally {
                isRefreshing = false
            }
        }
    }
}
