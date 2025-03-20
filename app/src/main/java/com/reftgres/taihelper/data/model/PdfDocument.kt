package com.reftgres.taihelper.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Модель документа PDF, хранимого в Firebase
 */
data class PdfDocument(
    @DocumentId
    val documentId: String = "",
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val fileUrl: Any = "",
    val thumbnailUrl: String = "",
    val fileSize: Double = 0.0,
    val pageCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val tags: List<String> = emptyList(),
    val isDownloaded: Boolean = false,
    val localPath: String = "",
    val lastOpenedPage: Int = 0
) {
    fun getFileUrlAsString(): String {
        return when (fileUrl) {
            is String -> fileUrl as String
            is com.google.firebase.firestore.DocumentReference -> {
                "https://avtomatica.ru/images/catalog/agk/agk-3101/agk-3101mas_re.pdf"
            }
            else -> ""
        }
    }
}