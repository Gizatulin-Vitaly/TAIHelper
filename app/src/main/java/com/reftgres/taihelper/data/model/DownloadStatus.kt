package com.reftgres.taihelper.data.model

sealed class DownloadStatus {
    object NotStarted : DownloadStatus()
    data class Progress(val progress: Int) : DownloadStatus()
    data class Success(val localFilePath: String) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}