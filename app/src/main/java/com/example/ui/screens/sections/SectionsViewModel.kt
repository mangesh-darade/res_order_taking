package com.example.ui.screens.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Section
import com.example.data.model.Subsection
import com.example.data.repository.AuthRepository
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionsUiState(
    val isLoading: Boolean = false,
    val sections: List<Section> = emptyList(),
    val filteredSections: List<Section> = emptyList(),
    val selectedSection: Section? = null,
    val subsections: List<Subsection> = emptyList(),
    val filteredSubsections: List<Subsection> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class SectionsViewModel(
    private val repository: RestaurantRepository = RestaurantRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionsUiState())
    val uiState: StateFlow<SectionsUiState> = _uiState.asStateFlow()

    init {
        loadSections()
    }

    fun loadSections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            AuthRepository.getInstance().refreshUserSession()
            repository.syncFloorPlanIfOnline(force = true)
            val result = repository.fetchSections()
            result.onSuccess { list ->
                val firstSection = list.firstOrNull()
                val query = _uiState.value.searchQuery.trim()
                val filtered = if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sections = list,
                    filteredSections = filtered,
                    selectedSection = firstSection
                )
                if (firstSection != null) {
                    selectSection(firstSection)
                }
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load sections"
                )
            }
        }
    }

    fun selectSection(section: Section) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedSection = section, isLoading = true)
            val result = repository.fetchSubsections(section.id)
            result.onSuccess { subList ->
                val query = _uiState.value.searchQuery.trim()
                val filtered = if (query.isBlank()) subList else subList.filter { it.name.contains(query, ignoreCase = true) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    subsections = subList,
                    filteredSubsections = filtered
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    subsections = emptyList(),
                    filteredSubsections = emptyList()
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val q = query.trim()
        val sections = _uiState.value.sections
        val subsections = _uiState.value.subsections

        val filteredSecs = if (q.isBlank()) {
            sections
        } else {
            sections.filter { it.name.contains(q, ignoreCase = true) }
        }

        val filteredSubs = if (q.isBlank()) {
            subsections
        } else {
            subsections.filter { it.name.contains(q, ignoreCase = true) }
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredSections = filteredSecs,
            filteredSubsections = filteredSubs
        )
    }
}
