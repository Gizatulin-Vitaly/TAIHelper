package com.reftgres.taihelper.ui.reference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.reftgres.taihelper.data.model.DocumentCategory
import com.reftgres.taihelper.data.model.PdfDocument
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.data.repository.PdfDocumentRepository
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
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
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories


    // LiveData для выбранной категории
    private val _selectedCategoryId = MutableLiveData<String?>(null)
    val selectedCategoryId: LiveData<String?> = _selectedCategoryId

    // LiveData для результатов поиска
    private val _searchResults = MutableLiveData<ResourceState<List<PdfDocument>>>()
    val searchResults: LiveData<ResourceState<List<PdfDocument>>> = _searchResults

    // LiveData для статуса сети
    private val _networkAvailable = MutableLiveData(networkService.isNetworkAvailable())
    val networkAvailable: LiveData<Boolean> = _networkAvailable


    init {
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

                if (result is ResourceState.Success) {
                    val cats = result.data.map { it.category }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    _categories.value = cats
                }

                filterDocuments()
            }
        }
    }



    /**
     * Сброс фильтра категории
     */
    fun clearCategoryFilter() {
        _selectedCategoryId.value = null
        filterDocuments()
    }


    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        filterDocuments()
    }

    private var currentQuery: String = ""

    fun updateSearchQuery(query: String) {
        currentQuery = query
        filterDocuments()
    }

    private fun filterDocuments() {
        val originalList = _documents.value
        val categoryId = _selectedCategoryId.value
        val query = currentQuery

        if (originalList is ResourceState.Success) {
            val filtered = originalList.data.filter {
                (categoryId == null || it.category == categoryId) &&
                        (query.isBlank() || it.title.contains(query, true) ||
                                it.description.contains(query, true) ||
                                it.tags.any { tag -> tag.contains(query, true) })
            }

            _searchResults.value = ResourceState.Success(filtered)
        }
    }

    /**
     * Поиск документов
     */
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            _searchResults.value = ResourceState.Loading
            val result = documentRepository.searchDocuments(query)
            _searchResults.value = result
        }
    }

    fun clearSearch() {
        currentQuery = ""
        filterDocuments()
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

    fun searchDocumentsLocally(query: String) {
        viewModelScope.launch {
            val originalList = _documents.value
            if (originalList is ResourceState.Success) {
                val result = documentRepository.searchDocumentsLocally(query, originalList.data)
                _searchResults.value = result
            }
        }
    }



    fun submitManualPdfDocument(title: String, description: String, category: String, fileUrl: String) {
        viewModelScope.launch {
            val doc = PdfDocument(
                documentId = "",
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                category = category,
                fileUrl = fileUrl,
                fileSize = 0.0,
                pageCount = 0,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                tags = listOf("")
            )
            documentRepository.uploadManualDocument(doc)
        }
    }

}