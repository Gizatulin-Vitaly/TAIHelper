package com.reftgres.taihelper.ui.reference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R
import java.io.File

class PdfPageAdapter(
    private val context: Context,
    private val pdfFile: File
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var pageCount: Int = 0

    init {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            pageCount = pdfRenderer?.pageCount ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pdf_view_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        // Не закрываем здесь currentPage

        // Открываем новую страницу
        pdfRenderer?.let { renderer ->
            val page = renderer.openPage(position)

            // Создаем Bitmap с необходимыми размерами
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )

            // Рендерим страницу на Bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // Устанавливаем Bitmap в ImageView
            holder.pdfPageImageView.setImageBitmap(bitmap)

            // Закрываем страницу после использования
            page.close()
        }
    }

    override fun getItemCount(): Int = pageCount

    class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pdfPageImageView: ImageView = itemView.findViewById(R.id.imageViewPage)
    }

    fun cleanup() {
        currentPage?.close()
        pdfRenderer?.close()
    }

}