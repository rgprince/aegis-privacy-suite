package com.aegis.privacy.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.FirewallRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * App info for firewall display.
 */
data class AppInfo(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isBlocked: Boolean
)

/**
 * ViewModel for per-app firewall.
 */
@HiltViewModel
class FirewallViewModel @Inject constructor(
    application: Application,
    private val database: AegisDatabase
) : AndroidViewModel(application) {
    
    private val packageManager = application.packageManager
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps = _showSystemApps.asStateFlow()
    
    private val firewallRules = database.firewallRuleDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val apps: StateFlow<List<AppInfo>> = combine(
        firewallRules,
        searchQuery,
        showSystemApps
    ) { rules, query, includeSystem ->
        loadApps(rules, query, includeSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private suspend fun loadApps(
        rules: List<FirewallRule>,
        query: String,
        includeSystem: Boolean
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val rulesMap = rules.associateBy { it.uid }
            
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    // Filter apps with INTERNET permission
                    val hasInternet = try {
                        packageManager.getPackageInfo(
                            app.packageName,
                            PackageManager.GET_PERMISSIONS
                        ).requestedPermissions?.contains(android.Manifest.permission.INTERNET) == true
                    } catch (e: Exception) {
                        false
                    }
                    
                    hasInternet && (includeSystem || (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0)
                }
                .map { app ->
                    val appName = packageManager.getApplicationLabel(app).toString()
                    val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val rule = rulesMap[app.uid]
                    
                    AppInfo(
                        uid = app.uid,
                        packageName = app.packageName,
                        appName = appName,
                        isSystemApp = isSystemApp,
                        isBlocked = rule?.blocked ?: false
                    )
                }
                .filter { app ->
                    query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Timber.e(e, "Error loading apps")
            emptyList()
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleShowSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
    }
    
    fun toggleAppBlock(app: AppInfo) {
        viewModelScope.launch {
            try {
                val rule = FirewallRule(
                    uid = app.uid,
                    packageName = app.packageName,
                    appName = app.appName,
                    blocked = !app.isBlocked
                )
                database.firewallRuleDao().insert(rule)
                Timber.i("Toggled firewall for ${app.appName}: ${rule.blocked}")
            } catch (e: Exception) {
                Timber.e(e, "Error toggling app firewall")
            }
        }
    }
}
