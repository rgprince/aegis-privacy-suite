package com.aegis.privacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.StatisticsEntity
import com.aegis.privacy.core.engine.BlocklistBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for dashboard screen.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val database: AegisDatabase,
    private val blocklistBridge: BlocklistBridge
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadStatistics()
        observeStatistics()
    }
    
    private fun observeStatistics() {
        viewModelScope.launch {
            database.statisticsDao().get()
                .filterNotNull()
                .collect { stats ->
                    _uiState.value = _uiState.value.copy(
                        totalDomains = stats.totalDomainsBlocked,
                        requestsBlocked = stats.totalRequestsBlocked,
                        requestsAllowed = stats.totalRequestsAllowed,
                        activeBlocklists = stats.blocklistCount,
                        customRules = stats.customRuleCount,
                        lastUpdated = stats.lastUpdated
                    )
                }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val stats = blocklistBridge.getStatistics()
                _uiState.value = _uiState.value.copy(
                    totalDomains = stats.totalDomainsBlocked,
                    requestsBlocked = stats.totalRequestsBlocked,
                    requestsAllowed = stats.totalRequestsAllowed,
                    activeBlocklists = stats.blocklistCount,
                    customRules = stats.customRuleCount,
                    lastUpdated = stats.lastUpdated,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading statistics")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun refreshStatistics() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadStatistics()
        _uiState.value = _uiState.value.copy(isRefreshing = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val totalDomains: Long = 0,
    val requestsBlocked: Long = 0,
    val requestsAllowed: Long = 0,
    val activeBlocklists: Int = 0,
    val customRules: Int = 0,
    val lastUpdated: Long = 0
) {
    val totalRequests: Long get() = requestsBlocked + requestsAllowed
    val blockRate: Float get() = if (totalRequests > 0) {
        (requestsBlocked.toFloat() / totalRequests.toFloat()) * 100f
    } else 0f
}
