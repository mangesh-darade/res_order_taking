package com.example.ui.screens.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Section
import com.example.data.model.Subsection
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionsUiState(
    val isLoading: Boolean = false,
    val sections: List<Section> = emptyList(),
    val selectedSection: Section? = null,
    val subsections: List<Subsection> = emptyList(),
    val errorMessage: String? = null
)

class SectionsViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionsUiState())
    val uiState: StateFlow<SectionsUiState> = _uiState.asStateFlow()

    init {
        loadSections()
    }

    fun loadSections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.syncFloorPlanIfOnline()
            val result = repository.fetchSections()
            result.onSuccess { list ->
                val firstSection = list.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sections = list,
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    subsections = subList
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    subsections = emptyList()
                )
            }
        }
    }
}
