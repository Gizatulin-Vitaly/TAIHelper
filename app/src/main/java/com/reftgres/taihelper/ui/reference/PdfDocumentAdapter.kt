package com.reftgres.taihelper.ui.reference

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reftgres.taihelper.R
import com.reftgres.taihelper.data.model.PdfDocument
import java.text.SimpleDateFormat
import java.util.Locale

class PdfDocumentAdapter(
    private val onDocumentClick: (PdfDocument) -> Unit
) : ListAdapter<PdfDocument, PdfDocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return DocumentViewHolder(view, onDocumentClick)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DocumentViewHolder(
        itemView: View,
        private val onDocumentClick: (PdfDocument) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.imageViewThumbnail)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        private val fileSizeTextView: TextView = itemView.findViewById(R.id.textViewFileSize)
        private val downloadedIndicator: ImageView = itemView.findViewById(R.id.imageViewDownloadedStatus)
        // Строку с optionsButton можно удалить или оставить неиспользуемой

        fun bind(document: PdfDocument) {
            titleTextView.text = document.title
            descriptionTextView.text = document.description

            // Отображение даты в нужном формате
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = document.updatedAt.toDate()
            dateTextView.text = dateFormat.format(date)

            // Отображение размера файла
            fileSizeTextView.text = formatFileSize(document.fileSize)

            // Показываем индикатор загрузки, если документ загружен локально
            downloadedIndicator.visibility = if (document.isDownloaded) View.VISIBLE else View.GONE

            // Загрузка превью с помощью Glide
            Glide.with(itemView.context)
                .load(document.thumbnailUrl)
                .placeholder(R.drawable.placeholder_pdf)
                .error(R.drawable.placeholder_pdf)
                .into(thumbnailImageView)

            // Обработка нажатия на элемент
            itemView.setOnClickListener {
                onDocumentClick(document)
            }

            // Удалена обработка нажатия на кнопку опций
        }

        private fun formatFileSize(sizeInBytes: Double): String {
            return when {
                sizeInBytes < 1024 -> "$sizeInBytes Б"
                sizeInBytes < 1024 * 1024 -> "${(sizeInBytes / 1024).toInt()} КБ"
                else -> String.format("%.1f МБ", sizeInBytes / (1024 * 1024))
            }
        }
    }

    class DocumentDiffCallback : DiffUtil.ItemCallback<PdfDocument>() {
        override fun areItemsTheSame(oldItem: PdfDocument, newItem: PdfDocument): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PdfDocument, newItem: PdfDocument): Boolean {
            return oldItem == newItem
        }
    }
}