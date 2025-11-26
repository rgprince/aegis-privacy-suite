package com.aegis.privacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.privacy.core.engine.BlocklistBridge
import com.aegis.privacy.core.engine.BlocklistBridgeFactory
import com.aegis.privacy.util.RootUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for settings and configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val bridgeFactory: BlocklistBridgeFactory,
    private val rootUtils: RootUtils
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val isRootAvailable = rootUtils.isRootAvailable()
                val isMagiskAvailable = rootUtils.isMagiskAvailable()
                val availableModes = bridgeFactory.getAvailableModes()
                val recommendedMode = bridgeFactory.getRecommendedMode()
                
                _uiState.value = _uiState.value.copy(
                    isRootAvailable = isRootAvailable,
                    isMagiskAvailable = isMagiskAvailable,
                    availableModes = availableModes,
                    recommendedMode = recommendedMode,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading settings")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun selectMode(mode: BlocklistBridge.Mode) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    selectedMode = mode,
                    isLoading = true
                )
                
                // Initialize the bridge with selected mode
                val (bridge, actualMode) = bridgeFactory.create(mode, getApplication())
                
                _uiState.value = _uiState.value.copy(
                    currentMode = actualMode,
                    isLoading = false
                )
                
                Timber.i("Mode changed to: $actualMode")
            } catch (e: Exception) {
                Timber.e(e, "Error selecting mode")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun requestRootAccess() {
        viewModelScope.launch {
            try {
                val hasRoot = rootUtils.isRootAvailable()
                _uiState.value = _uiState.value.copy(
                    isRootAvailable = hasRoot
                )
                if (hasRoot) {
                    Timber.i("Root access granted")
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Root access denied or unavailable"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error requesting root")
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
    fun toggleMagiskModule(enable: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                if (enable) {
                    // This will be done when applying blocklists
                    Timber.i("Magisk module will be enabled on next apply")
                } else {
                    rootUtils.removeMagiskModule()
                    Timber.i("Magisk module removed")
                }
                
                _uiState.value = _uiState.value.copy(
                    isMagiskModuleActive = enable,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Error toggling Magisk module")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun createBackup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = rootUtils.backupHostsFile()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        hasBackup = true,
                        isLoading = false
                    )
                    Timber.i("Backup created successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to create backup",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating backup")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun restoreBackup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = rootUtils.restoreHostsFile()
                if (result.isSuccess) {
                    rootUtils.restartDns()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Timber.i("Backup restored successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to restore backup",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error restoring backup")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentMode: BlocklistBridge.Mode? = null,
    val selectedMode: BlocklistBridge.Mode? = null,
    val availableModes: List<BlocklistBridge.Mode> = emptyList(),
    val recommendedMode: BlocklistBridge.Mode? = null,
    val isRootAvailable: Boolean = false,
    val isMagiskAvailable: Boolean = false,
    val isMagiskModuleActive: Boolean = false,
    val hasBackup: Boolean = false
)
