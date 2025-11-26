package com.aegis.privacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.BlocklistSourceEntity
import com.aegis.privacy.core.engine.BlocklistBridge
import com.aegis.privacy.core.engine.BlocklistRepository
import com.aegis.privacy.core.engine.BlocklistSource
import com.aegis.privacy.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for blocklist management.
 */
@HiltViewModel
class BlocklistViewModel @Inject constructor(
    application: Application,
    private val database: AegisDatabase,
    private val repository: BlocklistRepository,
    private val blocklistBridge: BlocklistBridge
) : AndroidViewModel(application) {
    
    val blocklists: StateFlow<List<BlocklistSourceEntity>> = database
        .blocklistSourceDao()
        .getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _uiState = MutableStateFlow(BlocklistUiState())
    val uiState: StateFlow<BlocklistUiState> = _uiState.asStateFlow()
    
    init {
        loadDefaultSources()
    }
    
    private fun loadDefaultSources() {
        viewModelScope.launch {
            try {
                // Add default sources if none exist
                if (blocklists.value.isEmpty()) {
                    addDefaultSources()
                    
                    // Auto-load OISD Basic blocklist for immediate functionality
                    delay(500) // Give database time to insert
                    Timber.i("Auto-loading OISD Basic blocklist...")
                    refreshSource("oisd_basic")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading default sources")
            }
        }
    }
    
    private suspend fun addDefaultSources() {
        val defaults = listOf(
            Constants.DefaultSources.STEVEN_BLACK,
            Constants.DefaultSources.ADAWAY,
            Constants.DefaultSources.OISD_BASIC
        )
        
        defaults.forEach { default ->
            val source = BlocklistSource(
                id = default.id,
                url = default.url,
                name = default.name,
                enabled = false // Disabled by default
            )
            repository.addSource(source)
        }
    }
    
    fun toggleSource(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val source = database.blocklistSourceDao().getById(sourceId)
                if (source != null) {
                    // Update enabled state
                    repository.updateSource(source.copy(enabled = enabled))
                    
                    // Auto-download if enabling and has 0 domains
                    if (enabled && source.domainCount == 0) {
                        Timber.i("Auto-downloading $sourceId after enabling...")
                        delay(200) // Brief delay for UI update
                        refreshSource(sourceId)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling source")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun refreshSource(sourceId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    refreshingSourceId = sourceId
                )
                
                val entity = database.blocklistSourceDao().getById(sourceId)
                if (entity != null) {
                    val source = BlocklistSource(
                        id = entity.id,
                        url = entity.url,
                        name = entity.name,
                        enabled = entity.enabled
                    )
                    
                    // Actually fetch and save the blocklist
                    Timber.i("Refreshing blocklist: ${source.name}")
                    val domains = repository.fetchBlocklist(source)
                    
                    if (domains.isNotEmpty()) {
                        repository.saveToDatabase(domains.toSet(), source.id)
                        Timber.i("Saved ${domains.size} domains from ${source.name}")
                        _uiState.value = _uiState.value.copy(
                            refreshingSourceId = null,
                            appliedSuccessfully = true
                        )
                    } else {
                        throw Exception("No domains fetched from ${source.url}")
                    }
                } else {
                    throw Exception("Source not found: $sourceId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing source")
                _uiState.value = _uiState.value.copy(
                    refreshingSourceId = null,
                    error = "Failed to refresh: ${e.message}"
                )
            }
        }
    }
    
    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSource(sourceId)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting source")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun applyChanges() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isApplying = true)
                
                // Load all enabled sources
                val enabledSources = blocklists.value
                    .filter { it.enabled }
                    .map { entity ->
                        BlocklistSource(
                            id = entity.id,
                            url = entity.url,
                            name = entity.name,
                            enabled = entity.enabled,
                            lastUpdated = entity.lastUpdated
                        )
                    }
                
                blocklistBridge.loadBlocklists(enabledSources)
                blocklistBridge.applyChanges()
                
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    appliedSuccessfully = true
                )
            } catch (e: Exception) {
                Timber.e(e, "Error applying changes")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isApplying = false
                )
            }
        }
    }
    
    fun addCustomSource(name: String, url: String) {
        viewModelScope.launch {
            try {
                // Generate ID from URL hash
                val id = "custom_${url.hashCode().toString(16)}"
                
                val source = BlocklistSourceEntity(
                    id = id,
                    name = name,
                    url = url,
                    enabled = false
                )
                
                database.blocklistSourceDao().insert(source)
                Timber.i("Added custom source: $name")
            } catch (e: Exception) {
                Timber.e(e, "Error adding custom source")
                _uiState.value = _uiState.value.copy(error = "Failed to add source: ${e.message}")
            }
        }
    }
    
    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(appliedSuccessfully = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BlocklistUiState(
    val isApplying: Boolean = false,
    val appliedSuccessfully: Boolean = false,
    val refreshingSourceId: String? = null,
    val error: String? = null
)
