package com.reftgres.taihelper.ui.reference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reftgres.taihelper.data.model.DownloadStatus
import com.reftgres.taihelper.data.model.PdfDocument
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.data.repository.PdfDocumentRepository
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val documentRepository: PdfDocumentRepository,
    private val networkService: NetworkConnectivityService
) : ViewModel() {

    // LiveData для текущего документа
    private val _document = MutableLiveData<ResourceState<PdfDocument>>()
    val document: LiveData<ResourceState<PdfDocument>> = _document

    // LiveData для статуса загрузки
    private val _downloadStatus = MutableLiveData<DownloadStatus>()
    val downloadStatus: LiveData<DownloadStatus> = _downloadStatus

    // LiveData для статуса сети
    private val _networkAvailable = MutableLiveData(networkService.isNetworkAvailable())
    val networkAvailable: LiveData<Boolean> = _networkAvailable

    init {
        // Наблюдаем за состоянием сети
        viewModelScope.launch {
            networkService.networkStatus.collect { isConnected ->
                _networkAvailable.value = isConnected
            }
        }
    }

    /**
     * Загрузка документа по ID
     */
    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _document.value = ResourceState.Loading
            val documentResult = documentRepository.getDocumentById(documentId)
            _document.value = documentResult

            // Если документ успешно загружен и уже скачан, сразу сообщаем о готовности файла
            if (documentResult is ResourceState.Success && documentResult.data.isDownloaded) {
                _downloadStatus.value = DownloadStatus.Success(documentResult.data.localPath)
            } else if (documentResult is ResourceState.Success) {
                // Если сеть доступна, автоматически начинаем загрузку
                if (networkService.isNetworkAvailable()) {
                    downloadDocument(documentResult.data)
                } else {
                    _downloadStatus.value = DownloadStatus.Error("Нет подключения к сети. Документ не загружен локально.")
                }
            }
        }
    }

    /**
     * Загрузка PDF файла
     */
    fun downloadDocument(document: PdfDocument) {
        viewModelScope.launch {
            documentRepository.downloadDocument(document).collect { status ->
                _downloadStatus.value = status
            }
        }
    }

    // Добавьте новый метод для загрузки по ID
    fun downloadDocumentById(documentId: String) {
        viewModelScope.launch {
            val documentResult = documentRepository.getDocumentById(documentId)
            if (documentResult is ResourceState.Success) {
                documentRepository.downloadDocument(documentResult.data).collect { status ->
                    _downloadStatus.value = status
                }
            } else {
                _downloadStatus.value = DownloadStatus.Error("Не удалось получить информацию о документе")
            }
        }
    }

    /**
     * Сохранение последней просмотренной страницы
     */
    fun saveLastViewedPage(documentId: String, pageNumber: Int) {
        viewModelScope.launch {
            documentRepository.saveLastOpenedPage(documentId, pageNumber)
        }
    }
}