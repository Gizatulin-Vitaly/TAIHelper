package com.reftgres.taihelper.ui.reference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.reftgres.taihelper.R
import com.reftgres.taihelper.data.model.DownloadStatus
import com.reftgres.taihelper.data.model.ResourceState
import com.reftgres.taihelper.databinding.PdfViewerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class PdfViewerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DOCUMENT_ID = "extra_document_id"

        fun createIntent(context: Context, documentId: String): Intent {
            return Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_ID, documentId)
            }
        }
    }

    private lateinit var binding: PdfViewerBinding
    private val viewModel: PdfViewerViewModel by viewModels()

    private var documentId: String? = null
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID)
        if (documentId == null) {
            showError("Идентификатор документа не найден")
            finish()
            return
        }

        // Загружаем информацию о документе
        viewModel.loadDocument(documentId!!)

        // Наблюдаем за данными
        observeViewModel()

        // Настраиваем кнопки навигации по страницам
        setupNavigationButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarPdf)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewModel() {
        // Наблюдаем за документом
        viewModel.document.observe(this) { result ->
            when (result) {
                is ResourceState.Loading -> {
                    binding.progressBarPdf.visibility = View.VISIBLE
                }
                is ResourceState.Success -> {
                    supportActionBar?.title = result.data.title

                    // Устанавливаем последнюю просмотренную страницу
                    currentPage = result.data.lastOpenedPage

                    // Если документ уже загружен локально, сразу отображаем его
                    if (result.data.isDownloaded) {
                        // Ничего не делаем, т.к. downloadStatus сработает
                    } else {
                        // Автоматически начинаем загрузку
                        viewModel.downloadDocument(result.data)
                    }
                }
                is ResourceState.Error -> {
                    binding.progressBarPdf.visibility = View.GONE
                    showError("Не удалось загрузить информацию о документе: ${result.message}")
                }
            }
        }

        // Наблюдаем за статусом загрузки
        viewModel.downloadStatus.observe(this) { status ->
            when (status) {
                is DownloadStatus.NotStarted -> {
                    binding.progressBarPdf.visibility = View.VISIBLE
                    binding.textViewProgressPdf.visibility = View.GONE
                }
                is DownloadStatus.Progress -> {
                    binding.progressBarPdf.visibility = View.VISIBLE
                    binding.textViewProgressPdf.visibility = View.VISIBLE
                    binding.textViewProgressPdf.text = "Загрузка файла: ${status.progress}%"
                }
                is DownloadStatus.Success -> {
                    binding.progressBarPdf.visibility = View.GONE
                    binding.textViewProgressPdf.visibility = View.GONE

                    // Отображаем PDF файл
                    displayPdf(status.localFilePath)
                }
                is DownloadStatus.Error -> {
                    binding.progressBarPdf.visibility = View.GONE
                    binding.textViewProgressPdf.visibility = View.GONE
                    showError("Ошибка при загрузке файла: ${status.message}")
                }
            }
        }

        // Наблюдаем за статусом сети
        viewModel.networkAvailable.observe(this) { isAvailable ->
            binding.networkStatusIndicator.visibility = if (isAvailable) View.GONE else View.VISIBLE
        }
    }

    private fun displayPdf(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                showError("Файл не найден")
                return
            }

            // Создаем адаптер
            val pdfAdapter = PdfPageAdapter(this, file)

            // Устанавливаем адаптер в ViewPager2
            binding.pdfViewPager.adapter = pdfAdapter

            // Восстанавливаем последнюю просмотренную страницу
            binding.pdfViewPager.setCurrentItem(currentPage, false)

            // Обработчик изменения страницы
            binding.pdfViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPage = position
                    updatePageNumber(position, pdfAdapter.itemCount)
                }
            })

            // Обновляем информацию о странице
            updatePageNumber(currentPage, pdfAdapter.itemCount)

        } catch (e: Exception) {
            showError("Ошибка отображения PDF: ${e.message}")
        }
    }

    private fun updatePageNumber(page: Int, pageCount: Int) {
        binding.textViewPageNumber.text = "Страница ${page + 1} из $pageCount"
        binding.buttonPrevPage.isEnabled = page > 0
        binding.buttonNextPage.isEnabled = page < pageCount - 1
    }

    private fun setupNavigationButtons() {
        binding.buttonPrevPage.setOnClickListener {
            if (currentPage > 0) {
                binding.pdfViewPager.currentItem = currentPage - 1
            }
        }

        binding.buttonNextPage.setOnClickListener {
            val adapter = binding.pdfViewPager.adapter
            if (adapter != null && currentPage < adapter.itemCount - 1) {
                binding.pdfViewPager.currentItem = currentPage + 1
            }
        }
    }

    private fun showError(message: String) {
        binding.textViewErrorPdf.visibility = View.VISIBLE
        binding.textViewErrorPdf.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()

        // Сохраняем текущую страницу перед выходом
        documentId?.let { id ->
            lifecycleScope.launch {
                viewModel.saveLastViewedPage(id, currentPage)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (binding.pdfViewPager.adapter as? PdfPageAdapter)?.cleanup()
    }
}