package com.reftgres.taihelper.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Модель категории документов
 */
data class DocumentCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val documentsCount: Int = 0
)