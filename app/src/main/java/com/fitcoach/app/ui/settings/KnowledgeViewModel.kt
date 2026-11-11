package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.local.entity.PlanVersionEntity
import com.fitcoach.app.data.local.entity.ScientificSourceEntity
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.ScientificSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val loading: Boolean = true,
    val sources: List<ScientificSourceEntity> = emptyList(),
    val versions: List<PlanVersionEntity> = emptyList()
)

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val scientificSourceRepository: ScientificSourceRepository,
    private val planVersionRepository: PlanVersionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(KnowledgeUiState())
    val state: StateFlow<KnowledgeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val sources = scientificSourceRepository.getAll()
            val versions = planVersionRepository.getAll()
            _state.value = KnowledgeUiState(loading = false, sources = sources, versions = versions)
        }
    }
}
