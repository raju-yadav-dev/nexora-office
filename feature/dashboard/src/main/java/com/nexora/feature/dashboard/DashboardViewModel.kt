package com.nexora.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.core.data.storage.RecentFilesRepository
import com.nexora.core.model.WorkspaceFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    recentFilesRepository: RecentFilesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        recentFilesRepository.observeRecentFiles()
            .onEach { files -> _state.value = _state.value.copy(recentFiles = files) }
            .launchIn(viewModelScope)
    }
}

data class DashboardState(
    val recentFiles: List<WorkspaceFile> = emptyList()
)
