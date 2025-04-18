package com.reftgres.taihelper.ui.reference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reftgres.taihelper.data.model.DocumentCategory
import com.reftgres.taihelper.data.model.PdfDocument
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.data.repository.PdfDocumentRepository
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfDocumentsViewModel @Inject constructor(
    private val documentRepository: PdfDocumentRepository,
    private val networkService: NetworkConnectivityService
) : ViewModel() {

    // LiveData для списка документов
    private val _documents = MutableLiveData<ResourceState<List<PdfDocument>>>()
    val documents: LiveData<ResourceState<List<PdfDocument>>> = _documents

    // LiveData для списка категорий
    private val _categories = MutableLiveData<List<DocumentCategory>>()
    val categories: LiveData<List<DocumentCategory>> = _categories

    // LiveData для выбранной категории
    private val _selectedCategoryId = MutableLiveData<String?>()
    val selectedCategoryId: LiveData<String?> = _selectedCategoryId

    // LiveData для результатов поиска
    private val _searchResults = MutableLiveData<ResourceState<List<PdfDocument>>>()
    val searchResults: LiveData<ResourceState<List<PdfDocument>>> = _searchResults

    // LiveData для статуса сети
    private val _networkAvailable = MutableLiveData(networkService.isNetworkAvailable())
    val networkAvailable: LiveData<Boolean> = _networkAvailable

    init {
        loadCategories()
        loadAllDocuments()

        // Наблюдаем за состоянием сети
        viewModelScope.launch {
            networkService.networkStatus.collect { isConnected ->
                _networkAvailable.value = isConnected
            }
        }
    }

    /**
     * Загрузка всех документов
     */
    fun loadAllDocuments() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { result ->
                _documents.value = result
            }
        }
    }

    /**
     * Загрузка документов по категории
     */
    fun loadDocumentsByCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId

        viewModelScope.launch {
            _documents.value = ResourceState.Loading
            documentRepository.getDocumentsByCategory(categoryId).collect { result ->
                _documents.value = result
            }
        }
    }

    /**
     * Сброс фильтра категории
     */
    fun clearCategoryFilter() {
        _selectedCategoryId.value = null
        loadAllDocuments()
    }

    /**
     * Поиск документов
     */
    fun searchDocuments(query: String) {
        if (query.isBlank()) {
            _searchResults.value = ResourceState.Success(emptyList())
            return
        }

        viewModelScope.launch {
            _searchResults.value = ResourceState.Loading
            _searchResults.value = documentRepository.searchDocuments(query)
        }
    }

    /**
     * Очистка результатов поиска
     */
    fun clearSearch() {
        _searchResults.value = ResourceState.Success(emptyList())
    }

    /**
     * Загрузка категорий
     */
    private fun loadCategories() {
        viewModelScope.launch {
            documentRepository.getAllCategories().collect { result ->
                when (result) {
                    is ResourceState.Success -> _categories.value = result.data ?: emptyList()

                    else -> { /* Обрабатываем в documents */ }
                }
            }
        }
    }

    /**
     * Получение размера кеша
     */
    suspend fun getCacheSize(): Long {
        return documentRepository.getLocalDocumentsCacheSize()
    }

    /**
     * Очистка кеша
     */
    suspend fun clearCache(): Boolean {
        return documentRepository.clearAllLocalDocuments()
    }

    /**
     * Удаление локального файла
     */
    suspend fun deleteLocalFile(documentId: String): Boolean {
        return documentRepository.deleteLocalFile(documentId)
    }
}