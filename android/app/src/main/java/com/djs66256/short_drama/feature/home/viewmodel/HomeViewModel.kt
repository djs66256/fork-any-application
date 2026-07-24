package com.djs66256.short_drama.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.config.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val appName: String = "",
    val appVersion: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    appConfig: AppConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = HomeUiState(
                isLoading = false,
                appName = appConfig.appName,
                appVersion = appConfig.appVersion
            )
        }
    }
}
