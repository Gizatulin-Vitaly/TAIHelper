package com.reftgres.taihelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.reftgres.taihelper.data.model.DocumentCategory
import com.reftgres.taihelper.data.model.DownloadStatus
import com.reftgres.taihelper.data.model.PdfDocument
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PdfDocumentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val networkService: NetworkConnectivityService,
    @ApplicationContext private val context: Context
) {
    private val TAG = "PdfDocumentRepository"

    // Коллекции Firestore
    private val documentsCollection = firestore.collection("pdf_documents")
    private val categoriesCollection = firestore.collection("document_categories")

    // SharedPreferences для хранения информации о документах
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("pdf_documents_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Получение всех документов
     */
    fun getAllDocuments(): Flow<ResourceState<List<PdfDocument>>> = callbackFlow {
        trySend(ResourceState.Loading)

        val listenerRegistration = documentsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting documents: ${error.message}")
                    trySend(ResourceState.Error("Ошибка при загрузке документов", error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val documents = snapshot.toObjects(PdfDocument::class.java)

                    // Проверяем локальное состояние документов
                    val updatedDocuments = documents.map { document ->
                        // Проверка, загружен ли документ локально
                        val localFile = getLocalFile(document.id)

                        // Получение последней открытой страницы
                        val lastPage = preferences.getInt("${document.id}_last_page", 0)

                        document.copy(
                            isDownloaded = localFile.exists(),
                            localPath = if (localFile.exists()) localFile.absolutePath else "",
                            lastOpenedPage = lastPage
                        )
                    }

                    trySend(ResourceState.Success(updatedDocuments))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Получение документов по категории
     */
    fun getDocumentsByCategory(categoryId: String): Flow<ResourceState<List<PdfDocument>>> = callbackFlow {
        trySend(ResourceState.Loading)

        val listenerRegistration = documentsCollection
            .whereEqualTo("category", categoryId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting documents by category: ${error.message}")
                    trySend(ResourceState.Error("Ошибка при загрузке документов", error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val documents = snapshot.toObjects(PdfDocument::class.java)

                    // Обогащаем документы локальной информацией
                    val updatedDocuments = documents.map { document ->
                        val localFile = getLocalFile(document.id)
                        val lastPage = preferences.getInt("${document.id}_last_page", 0)

                        document.copy(
                            isDownloaded = localFile.exists(),
                            localPath = if (localFile.exists()) localFile.absolutePath else "",
                            lastOpenedPage = lastPage
                        )
                    }

                    trySend(ResourceState.Success(updatedDocuments))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Получение документа по ID
     */
    suspend fun getDocumentById(documentId: String): ResourceState<PdfDocument> = withContext(Dispatchers.IO) {
        try {
            val document = documentsCollection.document(documentId).get().await()
                .toObject(PdfDocument::class.java)

            val localFile = document?.let { getLocalFile(it.id) }

            Log.d("PDF_DEBUG", "=== ДОКУМЕНТ ===")
            Log.d("PDF_DEBUG", "documentId = ${document?.documentId}")
            Log.d("PDF_DEBUG", "id (поле Firestore) = ${document?.id}")
            Log.d("PDF_DEBUG", "Ожидаемый путь к файлу: ${localFile?.absolutePath}")
            Log.d("PDF_DEBUG", "Файл существует: ${localFile?.exists()}")
            Log.d("PDF_DEBUG", "Список файлов в папке:")

            val dir = getPdfDirectory()
            dir.listFiles()?.forEach { file ->
                Log.d("PDF_DEBUG", "- ${file.name} (${file.length() / 1024} КБ)")
            }
            if (document != null) {
                Log.d(TAG, "Документ найден: ${document.title}")
                val localFile = getLocalFile(documentId)
                val lastPage = preferences.getInt("${documentId}_last_page", 0)

                ResourceState.Success(
                    document.copy(
                        isDownloaded = localFile.exists(),
                        localPath = if (localFile.exists()) localFile.absolutePath else "",
                        lastOpenedPage = lastPage
                    )
                )
            } else {
                Log.e(TAG, "Документ с ID $documentId не найден")
                ResourceState.Error("Документ не найден")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке документа: ${e.message}", e)
            ResourceState.Error("Ошибка при загрузке документа", e)
        }

    }

    /**
     * Скачивание PDF файла
     */
    fun downloadDocument(document: PdfDocument): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.NotStarted)

        try {
            // Проверка локального файла
            val localFile = getLocalFile(document.id)
            if (localFile.exists()) {
                emit(DownloadStatus.Success(localFile.absolutePath))
                return@flow
            }

            // Проверка сети
            if (!networkService.isNetworkAvailable()) {
                emit(DownloadStatus.Error("Нет подключения к сети"))
                return@flow
            }

            // Создание директории
            val pdfDir = getPdfDirectory()
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }

            // Получение URL как строки
            val fileUrlString = document.getFileUrlAsString()

            if (fileUrlString.isEmpty()) {
                emit(DownloadStatus.Error("Не удалось получить URL файла"))
                return@flow
            }

            // Загрузка файла по HTTP
            try {
                val url = java.net.URL(fileUrlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength

                val input = java.io.BufferedInputStream(url.openStream())
                val output = java.io.FileOutputStream(localFile)

                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count

                    // Публикуем прогресс
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        emit(DownloadStatus.Progress(progress))
                    }

                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                emit(DownloadStatus.Success(localFile.absolutePath))
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading via HTTP: ${e.message}")
                emit(DownloadStatus.Error("Ошибка загрузки: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading document: ${e.message}")
            emit(DownloadStatus.Error(e.message ?: "Неизвестная ошибка при загрузке файла"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Получение всех категорий
     */
    fun getAllCategories(): Flow<ResourceState<List<DocumentCategory>>> = callbackFlow {
        trySend(ResourceState.Loading)

        val listenerRegistration = categoriesCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting categories: ${error.message}")
                    trySend(ResourceState.Error("Ошибка при загрузке категорий", error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val categories = snapshot.toObjects(DocumentCategory::class.java)
                    trySend(ResourceState.Success(categories))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun searchDocumentsLocally(query: String, allDocs: List<PdfDocument>): ResourceState<List<PdfDocument>> {
        return try {
            val filtered = allDocs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
            ResourceState.Success(filtered)
        } catch (e: Exception) {
            ResourceState.Error(e.message ?: "Ошибка при локальном поиске")
        }
    }


    /**
     * Поиск документов
     */
    suspend fun searchDocuments(query: String): ResourceState<List<PdfDocument>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext ResourceState.Success(emptyList())
            }

            // Поиск по заголовку
            val titleResult = documentsCollection
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + "\uf8ff")
                .get()
                .await()
                .toObjects(PdfDocument::class.java)

            // Поиск по описанию
            val descriptionResult = documentsCollection
                .whereGreaterThanOrEqualTo("description", query)
                .whereLessThanOrEqualTo("description", query + "\uf8ff")
                .get()
                .await()
                .toObjects(PdfDocument::class.java)

            // Поиск по тегам
            val tagResult = documentsCollection
                .whereArrayContains("tags", query.toLowerCase())
                .get()
                .await()
                .toObjects(PdfDocument::class.java)

            // Объединяем результаты и удаляем дубликаты
            val combinedResults = (titleResult + descriptionResult + tagResult).distinctBy { it.id }

            // Обогащаем локальной информацией
            val updatedResults = combinedResults.map { document ->
                val localFile = getLocalFile(document.id)
                val lastPage = preferences.getInt("${document.id}_last_page", 0)

                document.copy(
                    isDownloaded = localFile.exists(),
                    localPath = if (localFile.exists()) localFile.absolutePath else "",
                    lastOpenedPage = lastPage
                )
            }

            ResourceState.Success(updatedResults)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents: ${e.message}")
            ResourceState.Error("Ошибка при поиске документов", e)
        }
    }

    /**
     * Сохранение последней открытой страницы
     */
    fun saveLastOpenedPage(documentId: String, pageNumber: Int) {
        preferences.edit().putInt("${documentId}_last_page", pageNumber).apply()
    }

    /**
     * Удаление локального файла
     */
    suspend fun deleteLocalFile(documentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val localFile = getLocalFile(documentId)
            if (localFile.exists()) {
                localFile.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local file: ${e.message}")
            false
        }
    }

    /**
     * Получение размера всех локальных файлов
     */
    suspend fun getLocalDocumentsCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val pdfDir = getPdfDirectory()
            if (!pdfDir.exists()) {
                return@withContext 0L
            }

            var totalSize = 0L
            pdfDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }

            totalSize
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache size: ${e.message}")
            0L
        }
    }

    /**
     * Очистка кеша
     */
    suspend fun clearAllLocalDocuments(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDir = getPdfDirectory()
            if (!pdfDir.exists()) {
                return@withContext true
            }

            var success = true
            pdfDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.delete()) {
                    success = false
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
            false
        }
    }

    /**
     * Получение директории для PDF файлов
     */
    private fun getPdfDirectory(): File {
        return File(context.filesDir, "pdf_documents")
    }

    /**
     * Получение локального файла для документа
     */
    private fun getLocalFile(id: String): File {
        val pdfDir = getPdfDirectory()
        return File(pdfDir, "$id.pdf")
    }

    suspend fun uploadManualDocument(doc: PdfDocument) {
        documentsCollection.document(doc.id).set(doc).await()
    }
}